package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.application.view.AvailabilityView;
import com.doodle.scheduling.application.view.CommonAvailabilityView;
import com.doodle.scheduling.config.DoodleProperties;
import com.doodle.scheduling.domain.availability.AvailabilityCalculator;
import com.doodle.scheduling.domain.availability.MergedBlock;
import com.doodle.scheduling.domain.availability.SlotSlice;
import com.doodle.scheduling.domain.availability.TimeWindow;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.infrastructure.persistence.TimeSlotRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final TimeSlotRepository slots;
    private final ScheduleLocks locks;
    private final TimeWindowPolicy windows;
    private final SchedulingMetrics metrics;
    private final DoodleProperties.Availability limits;

    public AvailabilityService(
            TimeSlotRepository slots,
            ScheduleLocks locks,
            TimeWindowPolicy windows,
            SchedulingMetrics metrics,
            DoodleProperties properties) {
        this.slots = slots;
        this.locks = locks;
        this.windows = windows;
        this.metrics = metrics;
        this.limits = properties.availability();
    }

    @Transactional(readOnly = true)
    public AvailabilityView forUser(UUID userId, Instant from, Instant to) {
        windows.check(from, to);
        return metrics.timeAvailability("user", () -> loadUser(userId, from, to));
    }

    @Transactional(readOnly = true)
    public CommonAvailabilityView common(List<UUID> requestedUserIds, Instant from, Instant to) {
        windows.check(from, to);
        return metrics.timeAvailability("group", () -> loadCommon(requestedUserIds, from, to));
    }

    private AvailabilityView loadUser(UUID userId, Instant from, Instant to) {
        List<MergedBlock> blocks = merged(locks.require(userId), from, to);
        long freeMinutes = 0;
        long busyMinutes = 0;
        List<AvailabilityView.Block> view = new ArrayList<>();
        for (MergedBlock block : blocks) {
            long minutes = Duration.between(block.start(), block.end()).toMinutes();
            if (block.status() == SlotStatus.FREE) {
                freeMinutes += minutes;
            } else {
                busyMinutes += minutes;
            }
            view.add(new AvailabilityView.Block(block.start(), block.end(), block.status(), block.slotIds()));
        }
        return new AvailabilityView(userId, from, to, freeMinutes, busyMinutes, List.copyOf(view));
    }

    private CommonAvailabilityView loadCommon(List<UUID> requestedUserIds, Instant from, Instant to) {
        List<UUID> userIds = requestedUserIds.stream().distinct().toList();
        if (userIds.isEmpty()) {
            throw new SchedulingException(ErrorCode.INVALID_REQUEST, "At least one user id is required.");
        }
        if (userIds.size() > limits.maxUsers()) {
            throw new SchedulingException(
                    ErrorCode.INVALID_REQUEST,
                    "At most %d users can be compared at once.".formatted(limits.maxUsers()));
        }
        List<List<TimeWindow>> freeByUser = new ArrayList<>();
        for (UUID userId : userIds) {
            List<TimeWindow> free = merged(locks.require(userId), from, to).stream()
                    .filter(block -> block.status() == SlotStatus.FREE)
                    .map(block -> new TimeWindow(block.start(), block.end()))
                    .toList();
            freeByUser.add(free);
        }
        List<CommonAvailabilityView.Window> common = AvailabilityCalculator.intersectAll(freeByUser).stream()
                .map(window -> new CommonAvailabilityView.Window(window.start(), window.end()))
                .toList();
        return new CommonAvailabilityView(from, to, userIds, common);
    }

    private List<MergedBlock> merged(Calendar calendar, Instant from, Instant to) {
        List<SlotSlice> slices = slots.findSlices(calendar.getId(), from, to);
        return AvailabilityCalculator.clipAndMerge(slices, from, to);
    }
}
