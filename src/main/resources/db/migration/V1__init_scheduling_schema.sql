-- Schedules, slots, and meetings.
-- Slot ranges are half-open [start_at, end_at): adjacent slots may touch, overlapping ones may not.
-- btree_gist powers a per-schedule exclusion constraint so a concurrent write cannot
-- create an overlap that slipped past the application check.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id         UUID PRIMARY KEY,
    name       VARCHAR(120)  NOT NULL,
    email      VARCHAR(255)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE TABLE calendars (
    id         UUID PRIMARY KEY,
    owner_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT calendars_owner_unique UNIQUE (owner_id),
    CONSTRAINT calendars_owner_fk FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE meetings (
    id           UUID PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(2000),
    start_at     TIMESTAMPTZ   NOT NULL,
    end_at       TIMESTAMPTZ   NOT NULL,
    organizer_id UUID          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT meetings_time_order CHECK (end_at > start_at),
    CONSTRAINT meetings_organizer_fk FOREIGN KEY (organizer_id) REFERENCES users (id)
);

CREATE TABLE meeting_participants (
    meeting_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    PRIMARY KEY (meeting_id, user_id),
    CONSTRAINT meeting_participants_meeting_fk FOREIGN KEY (meeting_id) REFERENCES meetings (id) ON DELETE CASCADE,
    CONSTRAINT meeting_participants_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE time_slots (
    id          UUID PRIMARY KEY,
    calendar_id UUID         NOT NULL,
    start_at    TIMESTAMPTZ  NOT NULL,
    end_at      TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    role        VARCHAR(32)  NOT NULL,
    meeting_id  UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL,
    CONSTRAINT time_slots_calendar_fk FOREIGN KEY (calendar_id) REFERENCES calendars (id),
    CONSTRAINT time_slots_meeting_fk FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT time_slots_time_order CHECK (end_at > start_at),
    CONSTRAINT time_slots_status_check CHECK (status IN ('FREE', 'BUSY')),
    CONSTRAINT time_slots_role_check CHECK (role IN ('USER', 'MEETING_HOLD')),
    CONSTRAINT time_slots_shape_check CHECK (
        (role = 'MEETING_HOLD' AND meeting_id IS NOT NULL AND status = 'BUSY')
        OR (role = 'USER' AND meeting_id IS NULL)
        OR (role = 'USER' AND meeting_id IS NOT NULL AND status = 'BUSY')
    ),
    CONSTRAINT time_slots_no_overlap EXCLUDE USING gist (
        calendar_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
);

CREATE INDEX time_slots_calendar_start_idx ON time_slots (calendar_id, start_at);
CREATE INDEX time_slots_meeting_idx ON time_slots (meeting_id);
CREATE INDEX meetings_organizer_start_idx ON meetings (organizer_id, start_at);
CREATE INDEX meeting_participants_user_idx ON meeting_participants (user_id);

COMMENT ON TABLE calendars IS 'Per-user schedule aggregate. Not part of the public API.';
