package com.doodle.scheduling.api;

import com.doodle.scheduling.api.dto.CreateSlotRequest;
import com.doodle.scheduling.api.dto.UpdateSlotRequest;
import com.doodle.scheduling.api.dto.UpdateSlotStatusRequest;
import com.doodle.scheduling.application.SlotService;
import com.doodle.scheduling.application.view.PageView;
import com.doodle.scheduling.application.view.SlotView;
import com.doodle.scheduling.domain.model.SlotStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/slots")
@Tag(name = "Slots")
public class SlotController {

    private final SlotService slots;

    public SlotController(SlotService slots) {
        this.slots = slots;
    }

    @PostMapping
    @Operation(summary = "Create a slot with a duration in minutes")
    public ResponseEntity<SlotView> create(@PathVariable UUID userId, @Valid @RequestBody CreateSlotRequest request) {
        SlotView slot = slots.create(userId, request.startAt(), request.durationMinutes(), request.status());
        return ResponseEntity.created(URI.create("/api/v1/users/" + userId + "/slots/" + slot.id())).body(slot);
    }

    @GetMapping
    @Operation(summary = "List a user's slots")
    public PageView<SlotView> list(
            @PathVariable UUID userId,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "status", required = false) SlotStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return slots.list(userId, from, to, status, page, size);
    }

    @GetMapping("/{slotId}")
    @Operation(summary = "Get a slot")
    public SlotView get(@PathVariable UUID userId, @PathVariable UUID slotId) {
        return slots.get(userId, slotId);
    }

    @PutMapping("/{slotId}")
    @Operation(summary = "Change a slot's start and duration")
    public SlotView update(
            @PathVariable UUID userId,
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateSlotRequest request) {
        return slots.update(userId, slotId, request.startAt(), request.durationMinutes());
    }

    @PatchMapping("/{slotId}/status")
    @Operation(summary = "Mark a slot free or busy")
    public SlotView mark(
            @PathVariable UUID userId,
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateSlotStatusRequest request) {
        return slots.mark(userId, slotId, request.status());
    }

    @DeleteMapping("/{slotId}")
    @Operation(summary = "Delete a slot that is not booked as a meeting")
    public ResponseEntity<Void> delete(@PathVariable UUID userId, @PathVariable UUID slotId) {
        slots.delete(userId, slotId);
        return ResponseEntity.noContent().build();
    }
}
