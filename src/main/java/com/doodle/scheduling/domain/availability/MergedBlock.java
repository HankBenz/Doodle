package com.doodle.scheduling.domain.availability;

import com.doodle.scheduling.domain.model.SlotStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MergedBlock(Instant start, Instant end, SlotStatus status, List<UUID> slotIds) {
}
