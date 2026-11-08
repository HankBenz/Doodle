package com.doodle.scheduling.domain.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.doodle.scheduling.domain.model.SlotStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private static final Instant T8 = Instant.parse("2026-12-01T08:00:00Z");
    private static final Instant T9 = Instant.parse("2026-12-01T09:00:00Z");
    private static final Instant T10 = Instant.parse("2026-12-01T10:00:00Z");
    private static final Instant T11 = Instant.parse("2026-12-01T11:00:00Z");
    private static final Instant T12 = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant T13 = Instant.parse("2026-12-01T13:00:00Z");
    private static final Instant T14 = Instant.parse("2026-12-01T14:00:00Z");

    @Test
    void mergesAdjacentBlocksWithTheSameStatusAndClipsToTheWindow() {
        UUID early = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID busy = UUID.randomUUID();

        List<MergedBlock> blocks = AvailabilityCalculator.clipAndMerge(List.of(
                slice(early, T8, T9, SlotStatus.FREE),
                slice(first, T9, T10, SlotStatus.FREE),
                slice(second, T10, T12, SlotStatus.FREE),
                slice(busy, T12, T14, SlotStatus.BUSY)), T9, T13);

        assertThat(blocks).containsExactly(
                new MergedBlock(T9, T12, SlotStatus.FREE, List.of(first, second)),
                new MergedBlock(T12, T13, SlotStatus.BUSY, List.of(busy)));
    }

    @Test
    void doesNotMergeDifferentStatusesThatOnlyTouch() {
        List<MergedBlock> blocks = AvailabilityCalculator.clipAndMerge(List.of(
                slice(UUID.randomUUID(), T9, T10, SlotStatus.FREE),
                slice(UUID.randomUUID(), T10, T11, SlotStatus.BUSY)), T8, T14);

        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).status()).isEqualTo(SlotStatus.FREE);
        assertThat(blocks.get(1).status()).isEqualTo(SlotStatus.BUSY);
    }

    @Test
    void coversAWindowFromOneSlotOrSeveralTouchingSlots() {
        SlotSlice wide = slice(UUID.randomUUID(), T8, T14, SlotStatus.FREE);
        SlotSlice left = slice(UUID.randomUUID(), T9, T10, SlotStatus.FREE);
        SlotSlice right = slice(UUID.randomUUID(), T10, T12, SlotStatus.FREE);
        SlotSlice gap = slice(UUID.randomUUID(), T11, T12, SlotStatus.FREE);

        assertThat(AvailabilityCalculator.covers(List.of(wide), T10, T12)).isTrue();
        assertThat(AvailabilityCalculator.covers(List.of(left, right), T9, T12)).isTrue();
        assertThat(AvailabilityCalculator.covers(List.of(left, gap), T9, T12)).isFalse();
        assertThat(AvailabilityCalculator.covers(List.of(left), T10, T11)).isFalse();
    }

    @Test
    void intersectsFreeWindowsAcrossPeople() {
        List<TimeWindow> common = AvailabilityCalculator.intersectAll(List.of(
                List.of(new TimeWindow(T9, T12)),
                List.of(new TimeWindow(T10, T11), new TimeWindow(T11, T13)),
                List.of(new TimeWindow(T8, T14))));

        assertThat(common).containsExactly(new TimeWindow(T10, T12));
    }

    @Test
    void intersectionIsEmptyWhenAnyoneHasNoFreeTime() {
        List<TimeWindow> common = AvailabilityCalculator.intersectAll(List.of(
                List.of(new TimeWindow(T9, T12)),
                List.of()));

        assertThat(common).isEmpty();
    }

    private static SlotSlice slice(UUID id, Instant start, Instant end, SlotStatus status) {
        return new SlotSlice(id, start, end, status);
    }
}
