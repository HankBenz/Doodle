package com.doodle.scheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record UpdateSlotRequest(
        @NotNull Instant startAt,
        @NotNull @Positive Integer durationMinutes) {
}
