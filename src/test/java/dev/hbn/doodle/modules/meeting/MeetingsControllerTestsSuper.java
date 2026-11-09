package dev.hbn.doodle.modules.meeting;

import dev.hbn.doodle.TestsSuper;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class MeetingsControllerTestsSuper extends TestsSuper {

    protected final String urlUnderTest = "/meetings";

    protected MeetingsControllerTestsSuper(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
