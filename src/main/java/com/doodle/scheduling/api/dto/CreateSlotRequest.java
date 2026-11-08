package com.doodle.scheduling.api.dto;

import com.doodle.scheduling.domain.model.SlotStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record CreateSlotRequest(
        @NotNull Instant startAt,
        @NotNull @Positive Integer durationMinutes,
        SlotStatus status) {
}
