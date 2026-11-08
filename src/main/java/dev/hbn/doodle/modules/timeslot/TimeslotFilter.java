package dev.hbn.doodle.modules.timeslot;

import java.time.Instant;
import java.util.UUID;

public record TimeslotFilter(UUID ownerId, Instant begin, Instant end, TimeSlotStatus status) {
}
