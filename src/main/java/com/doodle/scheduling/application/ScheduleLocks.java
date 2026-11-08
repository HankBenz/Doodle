package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.infrastructure.persistence.CalendarRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Serializes slot writes per user. Callers that touch several users must lock them
 * in sorted id order so two bookings cannot deadlock on each other.
 */
@Component
public class ScheduleLocks {

    private final CalendarRepository calendars;

    public ScheduleLocks(CalendarRepository calendars) {
        this.calendars = calendars;
    }

    public Calendar require(UUID userId) {
        return calendars.findByOwner_Id(userId).orElseThrow(() -> missing(userId));
    }

    public Calendar lockOne(UUID userId) {
        UUID calendarId = calendars.findIdByOwnerId(userId).orElseThrow(() -> missing(userId));
        return calendars.lockById(calendarId).orElseThrow(() -> missing(userId));
    }

    public List<Calendar> lockAll(Collection<UUID> userIds) {
        List<Calendar> locked = new ArrayList<>();
        for (UUID userId : userIds.stream().distinct().sorted().toList()) {
            locked.add(lockOne(userId));
        }
        return locked;
    }

    private static SchedulingException missing(UUID userId) {
        return new SchedulingException(ErrorCode.USER_NOT_FOUND, "User %s does not exist.".formatted(userId));
    }
}
