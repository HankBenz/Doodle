package dev.hbn.doodle.modules.meeting;

import dev.hbn.doodle.modules.timeslot.TimeslotStatus;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingsControllerDeleteTests extends MeetingsControllerTestsSuper {

    private final MockMvc mockMvc;

    private final JsonMapper objectMapper;

    @Autowired
    MeetingsControllerDeleteTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc, JsonMapper objectMapper) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void whenDeletingMeetingDoesNotExistsThenReturnBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        MockHttpServletRequestBuilder requestBuilder = delete(urlUnderTest + "/1")
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isBadRequest())
                .andDo(print())
        ;
    }

    @Test
    void whenDeletingMeetingExistsThenDeleteAndSetTimeslotFreeAndReturnSuccess() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var ownerId = UUID.randomUUID();
        String insertTimeslotCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertTimeslotCommand.formatted(ownerId.toString(), today(10), today(16), TimeslotStatus.BUSY.name()));

        String insertMeetingCommand = "INSERT INTO meetings (timeslot_id, title, description) VALUES (1, 'title', 'description')";
        jdbcTemplate.execute(insertMeetingCommand);

        MockHttpServletRequestBuilder requestBuilder = delete(urlUnderTest + "/1")
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);

        // Act
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM meetings", Integer.class));
        assertEquals("FREE", jdbcTemplate.queryForObject("SELECT status FROM timeslots WHERE id = 1", String.class));
    }
}
