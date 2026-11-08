package com.doodle.scheduling.domain.availability;

import com.doodle.scheduling.domain.model.SlotStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pure interval math for free/busy views. Ranges are half-open: a block that ends
 * at T and a block that starts at T touch and, when they share a status, merge.
 */
public final class AvailabilityCalculator {

    private AvailabilityCalculator() {
    }

    public static List<MergedBlock> clipAndMerge(List<SlotSlice> slots, Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        List<SlotSlice> clipped = new ArrayList<>();
        for (SlotSlice slot : slots) {
            Instant start = slot.start().isAfter(from) ? slot.start() : from;
            Instant end = slot.end().isBefore(to) ? slot.end() : to;
            if (start.isBefore(end)) {
                clipped.add(new SlotSlice(slot.id(), start, end, slot.status()));
            }
        }
        clipped.sort(Comparator.comparing(SlotSlice::start).thenComparing(slice -> slice.status().name()));

        List<MergedBlock> merged = new ArrayList<>();
        for (SlotSlice slot : clipped) {
            if (merged.isEmpty()) {
                merged.add(block(slot));
                continue;
            }
            MergedBlock current = merged.get(merged.size() - 1);
            boolean sameStatus = current.status() == slot.status();
            boolean touches = !slot.start().isAfter(current.end());
            if (sameStatus && touches) {
                Instant end = slot.end().isAfter(current.end()) ? slot.end() : current.end();
                List<UUID> ids = new ArrayList<>(current.slotIds());
                if (!ids.contains(slot.id())) {
                    ids.add(slot.id());
                }
                merged.set(merged.size() - 1, new MergedBlock(current.start(), end, current.status(), List.copyOf(ids)));
            } else {
                merged.add(block(slot));
            }
        }
        return List.copyOf(merged);
    }

    /**
     * True when free slices cover every instant of {@code [start, end)} with no gaps.
     * Busy slices are ignored here; callers reject them before asking.
     */
    public static boolean covers(List<SlotSlice> slots, Instant start, Instant end) {
        if (!start.isBefore(end)) {
            return false;
        }
        List<SlotSlice> free = slots.stream()
                .filter(slot -> slot.status() == SlotStatus.FREE)
                .sorted(Comparator.comparing(SlotSlice::start))
                .toList();
        Instant cursor = start;
        for (SlotSlice slot : free) {
            if (!slot.end().isAfter(cursor)) {
                continue;
            }
            if (slot.start().isAfter(cursor)) {
                return false;
            }
            cursor = slot.end();
            if (!cursor.isBefore(end)) {
                return true;
            }
        }
        return !cursor.isBefore(end);
    }

    public static List<TimeWindow> intersectAll(List<List<TimeWindow>> sets) {
        if (sets.isEmpty()) {
            return List.of();
        }
        List<TimeWindow> current = normalize(sets.get(0));
        for (int index = 1; index < sets.size(); index++) {
            current = intersectTwo(current, sets.get(index));
        }
        return current;
    }

    public static List<TimeWindow> normalize(List<TimeWindow> windows) {
        List<TimeWindow> sorted = windows.stream()
                .filter(window -> window.start().isBefore(window.end()))
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList();
        List<TimeWindow> merged = new ArrayList<>();
        for (TimeWindow window : sorted) {
            if (merged.isEmpty()) {
                merged.add(window);
                continue;
            }
            TimeWindow current = merged.get(merged.size() - 1);
            if (!window.start().isAfter(current.end())) {
                Instant end = window.end().isAfter(current.end()) ? window.end() : current.end();
                merged.set(merged.size() - 1, new TimeWindow(current.start(), end));
            } else {
                merged.add(window);
            }
        }
        return List.copyOf(merged);
    }

    private static List<TimeWindow> intersectTwo(List<TimeWindow> left, List<TimeWindow> right) {
        List<TimeWindow> first = normalize(left);
        List<TimeWindow> second = normalize(right);
        List<TimeWindow> intersection = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < first.size() && j < second.size()) {
            TimeWindow x = first.get(i);
            TimeWindow y = second.get(j);
            Instant start = x.start().isAfter(y.start()) ? x.start() : y.start();
            Instant end = x.end().isBefore(y.end()) ? x.end() : y.end();
            if (start.isBefore(end)) {
                intersection.add(new TimeWindow(start, end));
            }
            if (!x.end().isAfter(y.end())) {
                i++;
            } else {
                j++;
            }
        }
        return List.copyOf(intersection);
    }

    private static MergedBlock block(SlotSlice slot) {
        return new MergedBlock(slot.start(), slot.end(), slot.status(), List.of(slot.id()));
    }
}
