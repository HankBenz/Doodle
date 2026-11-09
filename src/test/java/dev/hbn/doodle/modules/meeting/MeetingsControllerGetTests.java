package dev.hbn.doodle.modules.meeting;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingsControllerGetTests extends MeetingsControllerTestsSuper {

    private final MockMvc mockMvc;

    @Autowired
    MeetingsControllerGetTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
    }

    @Test
    void whenMeetingsIsEmptyThenReturnEmptyPage() throws Exception {
        // Arrange
        String accessToken = readerToken();

        // Act
        MockHttpServletRequestBuilder requestBuilder = get(urlUnderTest)
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
        ;

    }

    @Test
    void whenMeetingsAreNotEmptyThenReturnAllMeetings() throws Exception {
        // Arrange
        String accessToken = readerToken();
        Instant today = Instant.now().atZone(utc).toLocalDate().atStartOfDay(utc).toInstant();
        Instant start = today.plusSeconds(10 * 3600);
        Instant finish = today.plusSeconds(16 * 3600);
        jdbcTemplate.execute("INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')".formatted(
                UUID.randomUUID().toString(),
                start,
                finish,
                MeetingStatus.FREE.name()
        ));
        jdbcTemplate.execute("INSERT INTO meetings (timeslot_id, title, description) VALUES (1, 'Meeting Title', 'Meeting Description')");
        jdbcTemplate.execute("INSERT INTO meeting_participant (meeting_id, participant_id) VALUES (1, '%s')".formatted(UUID.randomUUID()));

        // Act
        MockHttpServletRequestBuilder requestBuilder = get(urlUnderTest)
                .accept(String.valueOf(MediaType.APPLICATION_JSON))
                .header("Authorization", "Bearer " + accessToken);
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Meeting Title"))
                .andExpect(jsonPath("$.content[0].description").value("Meeting Description"))
                .andExpect(jsonPath("$.content[0].start").value(start.toString()))
                .andExpect(jsonPath("$.content[0].finish").value(finish.toString()))
                .andExpect(jsonPath("$.content[0].participants").isArray())
                .andExpect(jsonPath("$.content[0].participants.length()").value(1))
        ;

    }
}