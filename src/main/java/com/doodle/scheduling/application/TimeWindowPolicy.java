package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.config.DoodleProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TimeWindowPolicy {

    private final DoodleProperties.Availability availability;

    public TimeWindowPolicy(DoodleProperties properties) {
        this.availability = properties.availability();
    }

    public void check(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new SchedulingException(ErrorCode.INVALID_WINDOW, "Parameter 'from' must be before 'to'.");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(availability.maxWindowDays())) > 0) {
            throw new SchedulingException(
                    ErrorCode.INVALID_WINDOW,
                    "Time window cannot exceed %d days.".formatted(availability.maxWindowDays()));
        }
    }
}
