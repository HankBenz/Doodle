package dev.hbn.doodle.modules.timeslot;

import dev.hbn.doodle.TestsSuper;
import org.springframework.jdbc.core.JdbcTemplate;

public class TimeslotControllerTestsSuper extends TestsSuper {

    protected final String urlUnderTest = "/timeslots";

    protected TimeslotControllerTestsSuper(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
