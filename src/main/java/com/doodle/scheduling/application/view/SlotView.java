package com.doodle.scheduling.application.view;

import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.domain.model.TimeSlot;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record SlotView(
        UUID id,
        UUID userId,
        Instant startAt,
        Instant endAt,
        long durationMinutes,
        SlotStatus status,
        UUID meetingId) {

    public static SlotView from(TimeSlot slot, UUID userId) {
        return new SlotView(
                slot.getId(),
                userId,
                slot.getStartAt(),
                slot.getEndAt(),
                Duration.between(slot.getStartAt(), slot.getEndAt()).toMinutes(),
                slot.getStatus(),
                slot.linkedMeetingId());
    }
}
