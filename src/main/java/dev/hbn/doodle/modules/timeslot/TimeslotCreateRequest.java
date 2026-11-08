package dev.hbn.doodle.modules.timeslot;

import jakarta.validation.constraints.NotNull;
import net.minidev.json.annotate.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

public record TimeslotCreateRequest(
        @NotNull UUID ownerId,
        @NotNull Instant start,
        @NotNull Instant finish,
        @NotNull TimeSlotStatus status
) {
    @JsonIgnore
    public boolean isValid() {
        return ownerId != null && start != null && finish != null && status != null;
    }
}
