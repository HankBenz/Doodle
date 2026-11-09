package dev.hbn.doodle.modules.timeslot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record TimeslotUpdateRequest(
        @Positive long id,
        @NotNull UUID ownerId,
        @NotNull Instant start,
        @NotNull Instant finish,
        @NotNull TimeslotStatus status
) {
    public boolean isValid() {
        return ownerId != null && start != null && finish != null && status != null;
    }
}
