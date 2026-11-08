package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.domain.availability.AvailabilityCalculator;
import com.doodle.scheduling.domain.availability.SlotSlice;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.domain.model.Meeting;
import com.doodle.scheduling.domain.model.SlotRole;
import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.domain.model.TimeSlot;
import com.doodle.scheduling.infrastructure.persistence.TimeSlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SlotReserver {

    private final TimeSlotRepository slots;

    public SlotReserver(TimeSlotRepository slots) {
        this.slots = slots;
    }

    @Transactional
    public void reserveParticipant(Calendar calendar, UUID userId, Instant start, Instant end, Meeting meeting) {
        List<TimeSlot> overlapping = slots.findOverlapping(calendar.getId(), start, end);
        if (overlapping.stream().anyMatch(slot -> slot.getStatus() == SlotStatus.BUSY)) {
            throw new SchedulingException(
                    ErrorCode.PARTICIPANT_BUSY,
                    "User %s is busy during this slot.".formatted(userId));
        }
        if (!AvailabilityCalculator.covers(toSlices(overlapping), start, end)) {
            throw new SchedulingException(
                    ErrorCode.PARTICIPANT_NOT_AVAILABLE,
                    "User %s has not marked the whole slot as free.".formatted(userId));
        }

        List<TimeSlot> rightRemnants = new ArrayList<>();
        List<TimeSlot> fullyCovered = new ArrayList<>();
        for (TimeSlot slot : overlapping) {
            boolean extendsLeft = slot.getStartAt().isBefore(start);
            boolean extendsRight = slot.getEndAt().isAfter(end);
            if (extendsLeft && extendsRight) {
                Instant rightEnd = slot.getEndAt();
                slot.changeRange(slot.getStartAt(), start);
                rightRemnants.add(TimeSlot.userSlot(calendar, end, rightEnd, SlotStatus.FREE));
            } else if (extendsLeft) {
                slot.changeRange(slot.getStartAt(), start);
            } else if (extendsRight) {
                slot.changeRange(end, slot.getEndAt());
            } else {
                fullyCovered.add(slot);
            }
        }

        // Postgres checks the exclusion constraint per statement. Range changes must
        // be written before the busy hold is inserted, or the old free ranges still overlap it.
        slots.deleteAll(fullyCovered);
        slots.flush();
        slots.saveAll(rightRemnants);
        slots.flush();
        slots.save(TimeSlot.meetingHold(calendar, start, end, meeting));
    }

    @Transactional
    public void release(Meeting meeting) {
        List<TimeSlot> linked = slots.findByMeetingId(meeting.getId());
        List<FreedWindow> holds = new ArrayList<>();
        for (TimeSlot slot : linked) {
            if (slot.getRole() == SlotRole.MEETING_HOLD) {
                holds.add(new FreedWindow(slot.getCalendar().getId(), slot.getStartAt(), slot.getEndAt()));
                slot.convertHoldToFree();
            } else {
                slot.releaseFromMeeting();
            }
        }
        slots.flush();
        for (FreedWindow hold : holds) {
            coalesce(hold.calendarId(), hold.start(), hold.end());
        }
    }

    private void coalesce(UUID calendarId, Instant start, Instant end) {
        List<TimeSlot> touching = new ArrayList<>(slots.findFreeTouching(calendarId, start, end));
        touching.sort(Comparator.comparing(TimeSlot::getStartAt));
        List<TimeSlot> cluster = new ArrayList<>();
        for (TimeSlot slot : touching) {
            if (cluster.isEmpty() || !slot.getStartAt().isAfter(cluster.get(cluster.size() - 1).getEndAt())) {
                cluster.add(slot);
                continue;
            }
            mergeCluster(cluster);
            cluster = new ArrayList<>();
            cluster.add(slot);
        }
        mergeCluster(cluster);
    }

    private void mergeCluster(List<TimeSlot> cluster) {
        if (cluster.size() < 2) {
            return;
        }
        TimeSlot keeper = cluster.get(0);
        Instant start = keeper.getStartAt();
        Instant end = keeper.getEndAt();
        for (TimeSlot slot : cluster) {
            if (slot.getStartAt().isBefore(start)) {
                start = slot.getStartAt();
            }
            if (slot.getEndAt().isAfter(end)) {
                end = slot.getEndAt();
            }
        }
        slots.deleteAll(cluster.subList(1, cluster.size()));
        slots.flush();
        keeper.changeRange(start, end);
        slots.flush();
    }

    private static List<SlotSlice> toSlices(List<TimeSlot> slots) {
        return slots.stream()
                .map(slot -> new SlotSlice(slot.getId(), slot.getStartAt(), slot.getEndAt(), slot.getStatus()))
                .toList();
    }

    private record FreedWindow(UUID calendarId, Instant start, Instant end) {
    }
}
