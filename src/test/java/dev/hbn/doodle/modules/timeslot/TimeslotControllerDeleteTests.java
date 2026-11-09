package dev.hbn.doodle.modules.timeslot;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeslotControllerDeleteTests extends TimeslotControllerTestsSuper {

    private final MockMvc mockMvc;

    @Autowired
    TimeslotControllerDeleteTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
    }

    @Test
    void whenDeletingTimeslotDoesNotExistsThenSkipDeleteAndReturnSuccess() throws Exception {
        // Arrange
        String accessToken = readerToken();
        MockHttpServletRequestBuilder requestBuilder = delete(urlUnderTest + "/1")
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
        ;
    }

    @Test
    void whenDeletingTimeslotExistsThenDeleteAndReturnSuccess() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        MockHttpServletRequestBuilder requestBuilder = delete(urlUnderTest + "/1")
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
        ;
        String countCommand = "SELECT COUNT(*) FROM timeslots";
        assertEquals(0, jdbcTemplate.queryForObject(countCommand, Integer.class));
    }

    @Test
    void whenDeletingTimeslotIsUsedThenReturnFailure() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var today = java.time.Instant.now().atZone(utc).toLocalDate();
        var start = today.atTime(LocalTime.of(10, 0)).atZone(utc).toInstant();
        var finish = today.atTime(LocalTime.of(16, 0)).atZone(utc).toInstant();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), start, finish, TimeslotStatus.FREE.name()));

        String insertMeetingCommand = "INSERT INTO meetings (timeslot_id, title, description) VALUES (1, 'title', 'description')";
        jdbcTemplate.execute(insertMeetingCommand);

        MockHttpServletRequestBuilder requestBuilder = delete(urlUnderTest + "/1")
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.title").value("Invalid request"))
        ;
    }
}
