package com.doodle.scheduling.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateMeetingRequest(
        @NotNull UUID organizerId,
        @NotNull UUID slotId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull List<@NotNull UUID> participantIds) {
}
