package com.doodle.scheduling.application.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingView(
        UUID id,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        UUID organizerId,
        List<UUID> participantIds,
        UUID slotId) {
}
