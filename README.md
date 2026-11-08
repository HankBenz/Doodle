# Doodle scheduling

A small meeting scheduler. People publish the times they are free or busy, then book one free slot as a meeting with other people. The service answers two questions: when is one person free or busy, and when is a group free at the same time.

Times are UTC instants. Time with no slot is neither free nor busy, so a meeting can only be booked where every participant has explicitly marked that whole interval free.

## Run

Docker Compose starts Postgres and the service. The database user and password below are for this local demo only.

```bash
docker compose up --build
```

| | |
| --- | --- |
| API | http://localhost:9080 |
| OpenAPI UI | http://localhost:9080/swagger-ui.html |
| Health | http://localhost:9080/actuator/health |
| Prometheus | http://localhost:9080/actuator/prometheus |

The process listens on 8080 inside the container. The host port is 9080.

Stop and delete the database volume with `docker compose down -v`.

Commands below are single-line `curl` calls so they work in bash and in PowerShell (`curl.exe` on Windows, because `curl` is aliased to `Invoke-WebRequest`).

## Using the API

Create two people. Emails are stored in lower case.

```bash
curl -s -X POST http://localhost:9080/api/v1/users -H "Content-Type: application/json" -d "{\"name\":\"Ada Lovelace\",\"email\":\"ada@example.com\"}"
curl -s -X POST http://localhost:9080/api/v1/users -H "Content-Type: application/json" -d "{\"name\":\"Grace Hopper\",\"email\":\"grace@example.com\"}"
```

Each response includes an `id`. Publish free time with a start and a duration in minutes (15 to 720). Omit `status` to create a free slot, or set it to `BUSY` to block personal time.

```bash
curl -s -X POST http://localhost:9080/api/v1/users/{adaId}/slots -H "Content-Type: application/json" -d "{\"startAt\":\"2026-12-01T10:00:00Z\",\"durationMinutes\":60}"
curl -s -X POST http://localhost:9080/api/v1/users/{graceId}/slots -H "Content-Type: application/json" -d "{\"startAt\":\"2026-12-01T09:00:00Z\",\"durationMinutes\":180}"
```

Change a slot, mark it busy, or delete it:

```bash
curl -s -X PUT http://localhost:9080/api/v1/users/{adaId}/slots/{slotId} -H "Content-Type: application/json" -d "{\"startAt\":\"2026-12-01T10:30:00Z\",\"durationMinutes\":30}"
curl -s -X PATCH http://localhost:9080/api/v1/users/{adaId}/slots/{slotId}/status -H "Content-Type: application/json" -d "{\"status\":\"BUSY\"}"
curl -s -X DELETE http://localhost:9080/api/v1/users/{adaId}/slots/{slotId}
```

List slots in a window with `GET /api/v1/users/{userId}/slots?from=...&to=...&status=FREE&page=0&size=20`.

Ask for one person's aggregated free and busy blocks. Adjacent slots of the same status are merged, and the window is clipped to `from`/`to`.

```bash
curl -s "http://localhost:9080/api/v1/users/{adaId}/availability?from=2026-12-01T08:00:00Z&to=2026-12-01T18:00:00Z"
```

```json
{
  "userId": "...",
  "from": "2026-12-01T08:00:00Z",
  "to": "2026-12-01T18:00:00Z",
  "freeMinutes": 60,
  "busyMinutes": 0,
  "blocks": [
    {
      "startAt": "2026-12-01T10:00:00Z",
      "endAt": "2026-12-01T11:00:00Z",
      "status": "FREE",
      "slotIds": ["..."]
    }
  ]
}
```

`freeMinutes` and `busyMinutes` are whole minutes. Gaps are omitted: they are not free.

Find when every listed person is free. Pass ids as a comma-separated `userIds` value.

```bash
curl -s "http://localhost:9080/api/v1/availability?userIds={adaId},{graceId}&from=2026-12-01T08:00:00Z&to=2026-12-01T18:00:00Z"
```

Book Ada's free slot. The meeting takes that slot's whole interval. Grace must already be free for all of it; a single covering slot or several adjacent free slots both count. `participantIds` may be empty.

```bash
curl -s -X POST http://localhost:9080/api/v1/meetings -H "Content-Type: application/json" -d "{\"organizerId\":\"{adaId}\",\"slotId\":\"{adaSlotId}\",\"title\":\"Design review\",\"description\":\"Walk the schema\",\"participantIds\":[\"{graceId}\"]}"
```

Ada's slot stays the same id and becomes `BUSY`. Grace's covering free time is split so the meeting shows up as busy, and the leftover free time remains. Slots tied to a meeting cannot be edited or deleted.

Cancel returns that time to free. Only the organizer can cancel. If the meeting had been carved out of a longer free slot, the pieces are joined back together.

```bash
curl -s -X DELETE "http://localhost:9080/api/v1/meetings/{meetingId}?organizerId={adaId}"
```

List the meetings a person organizes or attends:

```bash
curl -s "http://localhost:9080/api/v1/users/{adaId}/meetings?from=2026-12-01T00:00:00Z&to=2026-12-02T00:00:00Z"
```

Failures use `application/problem+json` and a stable `code`:

| HTTP | code | when |
| --- | --- | --- |
| 400 | `INVALID_DURATION` | duration outside 15–720 minutes |
| 400 | `INVALID_WINDOW` | `from` is not before `to`, or the window is longer than 366 days |
| 400 | `INVALID_PARTICIPANTS` | organizer is listed again, or there are more than 50 participants |
| 400 | `INVALID_REQUEST` | malformed body, ids, or query |
| 403 | `NOT_ORGANIZER` | someone other than the organizer cancels |
| 404 | `USER_NOT_FOUND`, `SLOT_NOT_FOUND`, `MEETING_NOT_FOUND` | unknown id |
| 409 | `EMAIL_TAKEN` | email already registered |
| 409 | `SLOT_OVERLAP` | the new time overlaps a slot the user already has |
| 409 | `SLOT_LOCKED` | the slot belongs to a meeting |
| 409 | `SLOT_NOT_FREE` | the slot is already busy |
| 409 | `PARTICIPANT_BUSY` | a participant has a conflicting busy slot |
| 409 | `PARTICIPANT_NOT_AVAILABLE` | a participant has not marked the whole slot free |
| 409 | `LOCK_TIMEOUT`, `VERSION_CONFLICT` | retry the write |

There is no authentication. The caller supplies user ids. A production deployment would take the caller from a token and would not trust a client-supplied organizer id.

The interactive contract is the OpenAPI document at `/v3/api-docs`.

## Design

Each user owns one schedule aggregate. That aggregate is the unit of locking and the parent of every slot. It stays inside the domain: the HTTP API speaks in users, slots, availability, and meetings.

Slot ranges are half-open. A slot that ends at 10:00 may sit next to a slot that starts at 10:00. Overlaps are rejected twice: the write locks that user's schedule and checks, and Postgres rejects anything that still races through an exclusion constraint (`tstzrange` on `btree_gist`). The database constraint is the backstop; the application check is there to return a clear `409`.

Booking locks every involved user in sorted id order, so two meetings that share people cannot deadlock. The lock is the schedule row, not a global lock, so unrelated people book in parallel. The pool stays at 20 connections. Request handling uses virtual threads, and those threads wait on the pool instead of opening one database connection each.

A meeting books the organizer's slot in full. For each participant the service checks that free slots cover the interval with no gaps, trims or removes those slots, and inserts one busy hold. Cancelling a hold joins it back to the free time it was cut from. The organizer's slot id does not change. If one meeting covered several adjacent participant slots, cancel leaves a single free slot over that same time.

Availability reads project slot id, start, end, and status, then merge or intersect in memory. For a few hundred users and a few thousand slots, the hot path is an index range scan per person (`calendar_id, start_at`) plus a linear merge of the slots inside the requested window. A cache would have to be invalidated on every slot write, and the working set fits in Postgres, so there isn't one.

Reads and writes are separate transactions. Controllers return plain views, and the persistence session is not kept open for rendering (`open-in-view` is off). Schema changes go through Flyway; Hibernate only validates that the mappings match.

## Metrics

`/actuator/prometheus` exports the usual HTTP timers plus:

| meter | meaning |
| --- | --- |
| `doodle_slots_created_total` | slots created |
| `doodle_slots_overlap_total` | writes rejected as overlaps |
| `doodle_meetings_booked_total` | meetings booked |
| `doodle_meetings_cancelled_total` | meetings cancelled |
| `doodle_meetings_rejected_total{reason=...}` | booking rejected, by reason |
| `doodle_availability_query_seconds{scope="user"\|"group"}` | availability reads |

## Tests

Unit tests cover merging, clipping, coverage, and intersection. The API tests run against Postgres 16 in Testcontainers, including split and restore, busy and incomplete participants, common free time, and two concurrent bookings of one slot.

```bash
mvn test
```

Docker must be running. The build targets Java 21, which is also the runtime in the image.

## Out of scope

Recurring events, poll-style voting, notifications, reminders, and per-user permissions are not implemented. Authorization is the first thing to add before exposing this beyond a local demo.
