package com.doodle.scheduling.domain.availability;

import com.doodle.scheduling.domain.model.SlotStatus;
import java.time.Instant;
import java.util.UUID;

public record SlotSlice(UUID id, Instant start, Instant end, SlotStatus status) {
}
