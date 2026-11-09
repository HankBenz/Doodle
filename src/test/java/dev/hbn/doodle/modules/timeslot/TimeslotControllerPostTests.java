package dev.hbn.doodle.modules.timeslot;

import dev.hbn.doodle.TestsSuper;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeslotControllerPostTests extends TestsSuper {

    private final MockMvc mockMvc;

    private final JsonMapper objectMapper;

    @Autowired
    TimeslotControllerPostTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc, JsonMapper objectMapper) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void whenTimeslotsIsEmptyThenCreateTimeslotAndReturn() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var content = new TimeslotCreateRequest(UUID.randomUUID(), start, finish, TimeslotStatus.FREE);

        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").exists())
                .andExpect(jsonPath("$.start").value(start.toString()))
                .andExpect(jsonPath("$.finish").value(finish.toString()))
                .andExpect(jsonPath("$.status").value("FREE"))
        ;
    }

    @Test
    void whenTimeslotHasPartialConflictThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(ownerId, start.minusSeconds(3600), finish.minusSeconds(3600), TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotIsExactAsAPreviousThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(ownerId, start, finish, TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotContainsAPreviousThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(ownerId, start.minusSeconds(3600), finish.plusSeconds(3600), TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotIsWithinAPreviousThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(ownerId, start.plusSeconds(3600), finish.minusSeconds(3600), TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotStartAndFinishAreEqualThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();

        var content = new TimeslotCreateRequest(ownerId, start, start, TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotIsInvalidThenBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();

        var content = new TimeslotCreateRequest(ownerId, finish, start, TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").exists())
        ;
    }

    @Test
    void whenTimeslotHasNotConflictThenCreate() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(ownerId, start.plusSeconds(86400), finish.plusSeconds(86400), TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").exists())
                .andExpect(jsonPath("$.start").value(start.plus(1, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$.finish").value(finish.plus(1, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$.status").value("FREE"))
        ;
    }

    @Test
    void whenTimeslotBelongsToAnotherUserThenCreate() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(UUID.randomUUID(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotCreateRequest(UUID.randomUUID(), start, finish, TimeslotStatus.FREE);
        MockHttpServletRequestBuilder requestBuilder = post("/timeslots")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(content));

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").exists())
                .andExpect(jsonPath("$.start").value(start.toString()))
                .andExpect(jsonPath("$.finish").value(finish.toString()))
                .andExpect(jsonPath("$.status").value("FREE"))
        ;
    }
}
