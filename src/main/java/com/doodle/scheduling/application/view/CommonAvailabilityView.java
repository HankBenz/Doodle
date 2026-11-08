package com.doodle.scheduling.application.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommonAvailabilityView(
        Instant from,
        Instant to,
        List<UUID> userIds,
        List<Window> commonFree) {

    public record Window(Instant startAt, Instant endAt) {
    }
}
