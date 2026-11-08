package dev.hbn.doodle.modules.meeting;

import jakarta.validation.constraints.NotNull;
import net.minidev.json.annotate.JsonIgnore;

import java.util.List;
import java.util.UUID;

public record MeetingCreateRequest(
        @NotNull long timeslotId,
        @NotNull String title,
        @NotNull String description,
        @NotNull List<UUID> participants
) {
    @JsonIgnore
    public boolean isValid() {
        return timeslotId > 0 && isUsable(title) && isUsable(description) && !participants.isEmpty();
    }

    private boolean isUsable(@NotNull String text) {
        return text != null && !text.isEmpty();
    }
}
