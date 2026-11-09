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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeslotControllerPutTests extends TestsSuper {

    private final MockMvc mockMvc;

    private final JsonMapper objectMapper;

    @Autowired
    TimeslotControllerPutTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc, JsonMapper objectMapper) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void whenUpdatingTimeslotDoesNotExistsThenUpdateFails() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var content = new TimeslotUpdateRequest(1, UUID.randomUUID(), start, finish, TimeslotStatus.FREE);

        MockHttpServletRequestBuilder requestBuilder = put("/timeslots")
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
                .andExpect(jsonPath("$.detail").value("Timeslot with id 1 not found"))
        ;
    }

    @Test
    void whenUpdatingTimeslotHasConflictWithItselfThenReturnSuccess() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        var content = new TimeslotUpdateRequest(
                1,
                ownerId,
                start.minusSeconds(3600),
                finish.minusSeconds(3600),
                TimeslotStatus.FREE
        );
        MockHttpServletRequestBuilder requestBuilder = put("/timeslots")
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
                .andExpect(jsonPath("$.start").value(start.minusSeconds(3600).toString()))
                .andExpect(jsonPath("$.finish").value(finish.minusSeconds(3600).toString()))
                .andExpect(jsonPath("$.status").value("FREE"))
        ;
    }

    @Test
    void whenUpdatingTimeslotHasConflictWithAnotherThenReturnSuccess() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start.plus(1, ChronoUnit.DAYS), finish.plus(1, ChronoUnit.DAYS), TimeslotStatus.FREE.name()));

        var content = new TimeslotUpdateRequest(
                1,
                ownerId,
                start.plus(1, ChronoUnit.DAYS),
                finish.plus(1, ChronoUnit.DAYS),
                TimeslotStatus.FREE
        );
        MockHttpServletRequestBuilder requestBuilder = put("/timeslots")
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
}
