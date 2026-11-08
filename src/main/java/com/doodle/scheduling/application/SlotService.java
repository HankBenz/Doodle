package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.application.view.PageView;
import com.doodle.scheduling.application.view.SlotView;
import com.doodle.scheduling.config.DoodleProperties;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.domain.model.SlotRole;
import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.domain.model.TimeSlot;
import com.doodle.scheduling.infrastructure.persistence.TimeSlotRepository;
import com.doodle.scheduling.infrastructure.persistence.TimeSlotSpecifications;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final TimeSlotRepository slots;
    private final ScheduleLocks locks;
    private final TimeWindowPolicy windows;
    private final SchedulingMetrics metrics;
    private final DoodleProperties.Slot bounds;

    public SlotService(
            TimeSlotRepository slots,
            ScheduleLocks locks,
            TimeWindowPolicy windows,
            SchedulingMetrics metrics,
            DoodleProperties properties) {
        this.slots = slots;
        this.locks = locks;
        this.windows = windows;
        this.metrics = metrics;
        this.bounds = properties.slot();
    }

    @Transactional
    public SlotView create(UUID userId, Instant startAt, int durationMinutes, SlotStatus status) {
        Instant end = resolveEnd(startAt, durationMinutes);
        Calendar calendar = locks.lockOne(userId);
        ensureNoOverlap(calendar.getId(), startAt, end, null);
        TimeSlot slot = TimeSlot.userSlot(calendar, startAt, end, status == null ? SlotStatus.FREE : status);
        slots.save(slot);
        metrics.slotCreated();
        return SlotView.from(slot, userId);
    }

    @Transactional(readOnly = true)
    public SlotView get(UUID userId, UUID slotId) {
        Calendar calendar = locks.require(userId);
        return SlotView.from(load(calendar.getId(), slotId), userId);
    }

    @Transactional(readOnly = true)
    public PageView<SlotView> list(UUID userId, Instant from, Instant to, SlotStatus status, int page, int size) {
        if ((from == null) != (to == null)) {
            throw new SchedulingException(
                    ErrorCode.INVALID_WINDOW, "Parameters 'from' and 'to' must be provided together.");
        }
        if (from != null) {
            windows.check(from, to);
        }
        Calendar calendar = locks.require(userId);
        Page<SlotView> result = slots.findAll(
                        TimeSlotSpecifications.belongingTo(calendar.getId(), from, to, status),
                        PageRequests.of(page, size, Sort.by("startAt").and(Sort.by("id"))))
                .map(slot -> SlotView.from(slot, userId));
        return PageView.of(result);
    }

    @Transactional
    public SlotView update(UUID userId, UUID slotId, Instant startAt, int durationMinutes) {
        Instant end = resolveEnd(startAt, durationMinutes);
        Calendar calendar = locks.lockOne(userId);
        TimeSlot slot = loadEditable(calendar.getId(), slotId);
        ensureNoOverlap(calendar.getId(), startAt, end, slot.getId());
        slot.changeRange(startAt, end);
        return SlotView.from(slot, userId);
    }

    @Transactional
    public SlotView mark(UUID userId, UUID slotId, SlotStatus status) {
        Calendar calendar = locks.lockOne(userId);
        TimeSlot slot = loadEditable(calendar.getId(), slotId);
        slot.changeStatus(status);
        return SlotView.from(slot, userId);
    }

    @Transactional
    public void delete(UUID userId, UUID slotId) {
        Calendar calendar = locks.lockOne(userId);
        TimeSlot slot = loadEditable(calendar.getId(), slotId);
        slots.delete(slot);
    }

    private TimeSlot load(UUID calendarId, UUID slotId) {
        return slots.findByIdAndCalendar_Id(slotId, calendarId)
                .orElseThrow(() -> new SchedulingException(
                        ErrorCode.SLOT_NOT_FOUND, "Slot %s does not exist.".formatted(slotId)));
    }

    private TimeSlot loadEditable(UUID calendarId, UUID slotId) {
        TimeSlot slot = load(calendarId, slotId);
        if (slot.getRole() != SlotRole.USER || slot.getMeeting() != null) {
            throw new SchedulingException(
                    ErrorCode.SLOT_LOCKED, "Cancel the meeting before changing this slot.");
        }
        return slot;
    }

    private void ensureNoOverlap(UUID calendarId, Instant start, Instant end, UUID excludeId) {
        UUID excluded = excludeId == null ? new UUID(0L, 0L) : excludeId;
        if (!slots.findOverlappingIds(calendarId, start, end, excluded, PageRequest.of(0, 1)).isEmpty()) {
            metrics.slotOverlap();
            throw new SchedulingException(ErrorCode.SLOT_OVERLAP, "The time overlaps an existing slot.");
        }
    }

    private Instant resolveEnd(Instant start, int durationMinutes) {
        if (durationMinutes < bounds.minDurationMinutes() || durationMinutes > bounds.maxDurationMinutes()) {
            throw new SchedulingException(
                    ErrorCode.INVALID_DURATION,
                    "Duration must be between %d and %d minutes.".formatted(
                            bounds.minDurationMinutes(), bounds.maxDurationMinutes()));
        }
        return start.plus(durationMinutes, ChronoUnit.MINUTES);
    }
}
