package com.doodle.scheduling.application.view;

import com.doodle.scheduling.domain.model.SlotStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AvailabilityView(
        UUID userId,
        Instant from,
        Instant to,
        long freeMinutes,
        long busyMinutes,
        List<Block> blocks) {

    public record Block(Instant startAt, Instant endAt, SlotStatus status, List<UUID> slotIds) {
    }
}
