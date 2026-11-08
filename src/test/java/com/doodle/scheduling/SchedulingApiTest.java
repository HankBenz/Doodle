package com.doodle.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.doodle.scheduling.api.dto.CreateMeetingRequest;
import com.doodle.scheduling.api.dto.CreateSlotRequest;
import com.doodle.scheduling.api.dto.CreateUserRequest;
import com.doodle.scheduling.api.dto.UpdateSlotRequest;
import com.doodle.scheduling.api.dto.UpdateSlotStatusRequest;
import com.doodle.scheduling.application.view.MeetingView;
import com.doodle.scheduling.application.view.SlotView;
import com.doodle.scheduling.application.view.UserView;
import com.doodle.scheduling.domain.model.SlotStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
class SchedulingApiTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void exposesHealthMetricsAndOpenApi() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Doodle Scheduling API"));
    }

    @Test
    void managesSlotsAndAggregatesFreeAndBusyTime() throws Exception {
        UUID userId = createUser("Ada Lovelace");
        SlotView morning = createSlot(userId, "2026-12-01T09:00:00Z", 60);
        SlotView lateMorning = createSlot(userId, "2026-12-01T10:00:00Z", 60);
        createSlot(userId, "2026-12-01T12:00:00Z", 30, SlotStatus.BUSY);

        mockMvc.perform(put("/api/v1/users/{userId}/slots/{slotId}", userId, lateMorning.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateSlotRequest(Instant.parse("2026-12-01T10:30:00Z"), 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(30));

        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}/status", userId, morning.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateSlotStatusRequest(SlotStatus.BUSY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-12-01T08:00:00Z")
                        .param("to", "2026-12-01T14:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeMinutes").value(30))
                .andExpect(jsonPath("$.busyMinutes").value(90))
                .andExpect(jsonPath("$.blocks.length()").value(3))
                .andExpect(jsonPath("$.blocks[0].status").value("BUSY"))
                .andExpect(jsonPath("$.blocks[1].status").value("FREE"));

        mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateSlotRequest(Instant.parse("2026-12-01T09:30:00Z"), 15, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_OVERLAP"));

        mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateSlotRequest(Instant.parse("2026-12-01T15:00:00Z"), 10, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DURATION"));

        mockMvc.perform(delete("/api/v1/users/{userId}/slots/{slotId}", userId, morning.id()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/{userId}/slots/{slotId}", userId, morning.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
    }

    @Test
    void rejectsADuplicateEmailRegardlessOfCase() throws Exception {
        String email = "ada-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateUserRequest("Ada", email))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateUserRequest("Ada Again", email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void booksAMeetingWhenParticipantsCoverTheSlotAndRestoresThatTimeOnCancel() throws Exception {
        UUID ada = createUser("Ada");
        UUID grace = createUser("Grace");
        SlotView adaSlot = createSlot(ada, "2026-12-01T10:00:00Z", 60);
        createSlot(grace, "2026-12-01T09:00:00Z", 180);

        double bookedBefore = meterRegistry.counter("doodle.meetings.booked").count();
        MeetingView meeting = book(ada, adaSlot.id(), "Design review", "Walk the schema", grace);

        assertThat(meeting.organizerId()).isEqualTo(ada);
        assertThat(meeting.participantIds()).containsExactly(grace);
        assertThat(meeting.slotId()).isEqualTo(adaSlot.id());
        assertThat(meeting.startAt()).isEqualTo(Instant.parse("2026-12-01T10:00:00Z"));
        assertThat(meeting.description()).isEqualTo("Walk the schema");
        assertThat(meterRegistry.counter("doodle.meetings.booked").count()).isEqualTo(bookedBefore + 1);

        JsonNode graceSlots = slotsOf(grace);
        assertThat(startEnds(graceSlots)).containsExactly(
                "2026-12-01T09:00:00Z/2026-12-01T10:00:00Z/FREE",
                "2026-12-01T10:00:00Z/2026-12-01T11:00:00Z/BUSY",
                "2026-12-01T11:00:00Z/2026-12-01T12:00:00Z/FREE");

        mockMvc.perform(delete("/api/v1/users/{userId}/slots/{slotId}", ada, adaSlot.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_LOCKED"));

        mockMvc.perform(get("/api/v1/users/{userId}/meetings", grace)
                        .param("from", "2026-12-01T00:00:00Z")
                        .param("to", "2026-12-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(meeting.id().toString()))
                .andExpect(jsonPath("$[0].slotId").value(adaSlot.id().toString()));

        mockMvc.perform(get("/api/v1/meetings/{meetingId}", meeting.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(adaSlot.id().toString()));

        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meeting.id())
                        .param("organizerId", grace.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ORGANIZER"));

        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meeting.id())
                        .param("organizerId", ada.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}/slots/{slotId}", ada, adaSlot.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.meetingId").value(nullValue()));

        assertThat(startEnds(slotsOf(grace))).containsExactly(
                "2026-12-01T09:00:00Z/2026-12-01T12:00:00Z/FREE");
    }

    @Test
    void booksAcrossAdjacentFreeSlotsAndRejectsPeopleWhoAreNotFree() throws Exception {
        UUID ada = createUser("Ada");
        UUID grace = createUser("Grace");
        SlotView adaSlot = createSlot(ada, "2026-12-01T10:00:00Z", 60);
        createSlot(grace, "2026-12-01T10:00:00Z", 30);
        createSlot(grace, "2026-12-01T10:30:00Z", 30);

        MeetingView meeting = book(ada, adaSlot.id(), "Standup", null, grace);
        assertThat(meeting.slotId()).isEqualTo(adaSlot.id());

        UUID alan = createUser("Alan");
        SlotView alanSlot = createSlot(alan, "2026-12-01T13:00:00Z", 120);
        createSlot(grace, "2026-12-01T13:00:00Z", 60);

        mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateMeetingRequest(
                                alan, alanSlot.id(), "Planning", null, List.of(grace)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_NOT_AVAILABLE"));

        mockMvc.perform(get("/api/v1/users/{userId}/slots/{slotId}", alan, alanSlot.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    void rejectsAParticipantWhoIsBusyAndLeavesTheOrganizerSlotFree() throws Exception {
        UUID ada = createUser("Ada");
        UUID grace = createUser("Grace");
        SlotView adaSlot = createSlot(ada, "2026-12-01T10:00:00Z", 60);
        SlotView graceBusy = createSlot(grace, "2026-12-01T10:00:00Z", 60);
        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}/status", grace, graceBusy.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateSlotStatusRequest(SlotStatus.BUSY))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateMeetingRequest(
                                ada, adaSlot.id(), "Conflict", null, List.of(grace)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_BUSY"));

        mockMvc.perform(get("/api/v1/users/{userId}/slots/{slotId}", ada, adaSlot.id()))
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    void reportsCommonFreeTimeAndIgnoresTimeThatWasNeverMarkedFree() throws Exception {
        UUID ada = createUser("Ada");
        UUID grace = createUser("Grace");
        createSlot(ada, "2026-12-01T09:00:00Z", 180);
        createSlot(grace, "2026-12-01T10:00:00Z", 60);
        createSlot(grace, "2026-12-01T11:00:00Z", 120);

        mockMvc.perform(get("/api/v1/availability")
                        .param("userIds", ada + "," + grace)
                        .param("from", "2026-12-01T08:00:00Z")
                        .param("to", "2026-12-01T16:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commonFree.length()").value(1))
                .andExpect(jsonPath("$.commonFree[0].startAt").value("2026-12-01T10:00:00Z"))
                .andExpect(jsonPath("$.commonFree[0].endAt").value("2026-12-01T12:00:00Z"));
    }

    @Test
    void onlyOneConcurrentBookingOfASlotSucceeds() throws Exception {
        UUID ada = createUser("Ada");
        SlotView slot = createSlot(ada, "2026-12-01T15:00:00Z", 60);
        byte[] body = json(new CreateMeetingRequest(ada, slot.id(), "Race", null, List.of()));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MvcResult>> results = new ArrayList<>();
        for (int attempt = 0; attempt < 2; attempt++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/v1/meetings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn();
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<MvcResult> result : results) {
            MvcResult completed = result.get(30, TimeUnit.SECONDS);
            statuses.add(completed.getResponse().getStatus());
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);

        mockMvc.perform(get("/api/v1/users/{userId}/slots/{slotId}", ada, slot.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));
    }

    private UUID createUser(String name) throws Exception {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateUserRequest(name, slug + "-" + UUID.randomUUID() + "@example.com"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), UserView.class).id();
    }

    private SlotView createSlot(UUID userId, String start, int minutes) throws Exception {
        return createSlot(userId, start, minutes, null);
    }

    private SlotView createSlot(UUID userId, String start, int minutes, SlotStatus status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateSlotRequest(Instant.parse(start), minutes, status))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), SlotView.class);
    }

    private MeetingView book(UUID organizerId, UUID slotId, String title, String description, UUID... participantIds)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateMeetingRequest(
                                organizerId, slotId, title, description, List.of(participantIds)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), MeetingView.class);
    }

    private JsonNode slotsOf(UUID userId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/{userId}/slots", userId)
                        .param("from", "2026-12-01T00:00:00Z")
                        .param("to", "2026-12-02T00:00:00Z")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static List<String> startEnds(JsonNode page) {
        List<String> ranges = new ArrayList<>();
        for (JsonNode item : page.get("items")) {
            ranges.add(item.get("startAt").asText() + "/" + item.get("endAt").asText() + "/" + item.get("status").asText());
        }
        return ranges;
    }

    private byte[] json(Object value) throws Exception {
        return objectMapper.writeValueAsBytes(value);
    }
}
