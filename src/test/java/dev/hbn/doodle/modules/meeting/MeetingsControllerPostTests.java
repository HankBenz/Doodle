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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingsControllerPostTests extends MeetingsControllerTestsSuper {

    private final MockMvc mockMvc;

    private final JsonMapper objectMapper;

    @Autowired
    MeetingsControllerPostTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc, JsonMapper objectMapper) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void whenTimeslotDoesNotExistsThenCreateReturnBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var content = new MeetingCreateRequest(0, "Title", "Description", List.of(UUID.randomUUID()));

        MockHttpServletRequestBuilder requestBuilder = post(urlUnderTest)
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
        ;
    }

    @Test
    void whenTimeslotIsBusyThenReturnBadRequest() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), today(10), today(16), TimeslotStatus.BUSY.name()));

        var content = new MeetingCreateRequest(1, "Title", "Description", List.of(UUID.randomUUID()));
        MockHttpServletRequestBuilder requestBuilder = post(urlUnderTest)
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
        ;
    }

    @Test
    void whenTimeslotIsFreeThenCreateMeetingsAndReturnOk() throws Exception {
        // Arrange
        String accessToken = readerToken();
        var ownerId = UUID.randomUUID();
        String insertCommand = "INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')";
        jdbcTemplate.execute(insertCommand.formatted(ownerId.toString(), today(10), today(16), TimeslotStatus.FREE.name()));

        var content = new MeetingCreateRequest(1, "Title", "Description", List.of(UUID.randomUUID()));
        MockHttpServletRequestBuilder requestBuilder = post(urlUnderTest)
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
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.start").value(today(10).toString()))
                .andExpect(jsonPath("$.finish").value(today(16).toString()))
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(1))
        ;

        assertEquals("BUSY", jdbcTemplate.queryForObject("SELECT Status FROM timeslots WHERE id=1", String.class));
    }
}
