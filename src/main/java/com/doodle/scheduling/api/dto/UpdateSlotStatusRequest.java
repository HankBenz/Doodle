package com.doodle.scheduling.api.dto;

import com.doodle.scheduling.domain.model.SlotStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSlotStatusRequest(@NotNull SlotStatus status) {
}
