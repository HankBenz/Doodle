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

import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeslotControllerGetTests extends TestsSuper {

    private final MockMvc mockMvc;

    @Autowired
    TimeslotControllerGetTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc) {
        super(jdbcTemplate);
        this.mockMvc = mockMvc;
    }

    @Test
    void whenTimeslotsIsEmptyThenReturnEmptyPage() throws Exception {
        // Arrange
        String accessToken = readerToken();

        // Act
        MockHttpServletRequestBuilder requestBuilder = get("/timeslots")
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
    void whenTimeslotsIsNotEmptyThenReturnValues() throws Exception {
        // Arrange
        String accessToken = readerToken();
        jdbcTemplate.execute("INSERT INTO timeslots (owner_id, start, finish, status) VALUES ('%s', '%s', '%s', '%s')".formatted(
                UUID.randomUUID().toString(),
                java.time.Instant.now().atZone(utc).toLocalDate().atTime(LocalTime.of(10, 0)),
                java.time.Instant.now().atZone(utc).toLocalDate().atTime(LocalTime.of(16, 0)),
                TimeslotStatus.FREE.name()
        ));

        // Act
        MockHttpServletRequestBuilder requestBuilder = get("/timeslots").accept(String.valueOf(MediaType.APPLICATION_JSON)).header("Authorization", "Bearer " + accessToken);
        var actions = mockMvc.perform(requestBuilder);

        // Assert
        actions
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
        ;

    }
}