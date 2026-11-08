package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.application.view.MeetingView;
import com.doodle.scheduling.config.DoodleProperties;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.domain.model.Meeting;
import com.doodle.scheduling.domain.model.SlotRole;
import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.domain.model.TimeSlot;
import com.doodle.scheduling.domain.model.User;
import com.doodle.scheduling.infrastructure.persistence.MeetingRepository;
import com.doodle.scheduling.infrastructure.persistence.OrganizerSlotRef;
import com.doodle.scheduling.infrastructure.persistence.TimeSlotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetings;
    private final TimeSlotRepository slots;
    private final ScheduleLocks locks;
    private final SlotReserver reserver;
    private final TimeWindowPolicy windows;
    private final SchedulingMetrics metrics;
    private final DoodleProperties.Meeting limits;

    public MeetingService(
            MeetingRepository meetings,
            TimeSlotRepository slots,
            ScheduleLocks locks,
            SlotReserver reserver,
            TimeWindowPolicy windows,
            SchedulingMetrics metrics,
            DoodleProperties properties) {
        this.meetings = meetings;
        this.slots = slots;
        this.locks = locks;
        this.reserver = reserver;
        this.windows = windows;
        this.metrics = metrics;
        this.limits = properties.meeting();
    }

    @Transactional
    public MeetingView book(
            UUID organizerId,
            UUID slotId,
            String title,
            String description,
            List<UUID> participantIds) {
        List<UUID> participants = normalizeParticipants(organizerId, participantIds);
        List<UUID> lockIds = new ArrayList<>(participants);
        lockIds.add(organizerId);
        Map<UUID, Calendar> byOwner = index(locks.lockAll(lockIds));

        Calendar organizerCalendar = byOwner.get(organizerId);
        TimeSlot organizerSlot = slots.findByIdAndCalendar_Id(slotId, organizerCalendar.getId())
                .orElseThrow(() -> new SchedulingException(
                        ErrorCode.SLOT_NOT_FOUND, "Slot %s does not exist.".formatted(slotId)));
        if (organizerSlot.getStatus() != SlotStatus.FREE
                || organizerSlot.getMeeting() != null
                || organizerSlot.getRole() != SlotRole.USER) {
            metrics.meetingRejected("slot_not_free");
            throw new SchedulingException(ErrorCode.SLOT_NOT_FREE, "Only a free slot can be booked.");
        }

        String normalizedDescription = description == null || description.isBlank() ? null : description.trim();
        List<User> participantUsers = participants.stream()
                .map(id -> byOwner.get(id).getOwner())
                .toList();
        Meeting meeting = new Meeting(
                title.trim(),
                normalizedDescription,
                organizerSlot.getStartAt(),
                organizerSlot.getEndAt(),
                organizerCalendar.getOwner(),
                participantUsers);
        meetings.saveAndFlush(meeting);
        organizerSlot.attachMeeting(meeting);

        for (UUID participantId : participants) {
            try {
                reserver.reserveParticipant(
                        byOwner.get(participantId),
                        participantId,
                        meeting.getStartAt(),
                        meeting.getEndAt(),
                        meeting);
            } catch (SchedulingException ex) {
                metrics.meetingRejected(ex.code().name().toLowerCase(Locale.ROOT));
                throw ex;
            }
        }

        metrics.meetingBooked();
        log.info("Booked meeting {} on slot {}", meeting.getId(), organizerSlot.getId());
        return toView(meeting, organizerSlot.getId());
    }

    @Transactional(readOnly = true)
    public MeetingView get(UUID meetingId) {
        Meeting meeting = meetings.findById(meetingId)
                .orElseThrow(() -> new SchedulingException(
                        ErrorCode.MEETING_NOT_FOUND, "Meeting %s does not exist.".formatted(meetingId)));
        return toView(meeting, organizerSlotId(meeting.getId()));
    }

    @Transactional(readOnly = true)
    public List<MeetingView> list(UUID userId, Instant from, Instant to) {
        windows.check(from, to);
        locks.require(userId);
        Map<UUID, Meeting> byId = new LinkedHashMap<>();
        for (Meeting meeting : meetings.findOrganized(userId, from, to)) {
            byId.put(meeting.getId(), meeting);
        }
        for (Meeting meeting : meetings.findParticipating(userId, from, to)) {
            byId.putIfAbsent(meeting.getId(), meeting);
        }
        List<Meeting> ordered = byId.values().stream()
                .sorted(Comparator.comparing(Meeting::getStartAt).thenComparing(m -> m.getId().toString()))
                .toList();
        Map<UUID, UUID> slotIds = organizerSlotIds(ordered.stream().map(Meeting::getId).toList());
        return ordered.stream()
                .map(meeting -> toView(meeting, slotIds.get(meeting.getId())))
                .toList();
    }

    @Transactional
    public void cancel(UUID meetingId, UUID organizerId) {
        Meeting meeting = meetings.lockById(meetingId)
                .orElseThrow(() -> new SchedulingException(
                        ErrorCode.MEETING_NOT_FOUND, "Meeting %s does not exist.".formatted(meetingId)));
        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new SchedulingException(ErrorCode.NOT_ORGANIZER, "Only the organizer can cancel this meeting.");
        }
        List<UUID> userIds = new ArrayList<>();
        userIds.add(organizerId);
        meeting.getParticipants().forEach(user -> userIds.add(user.getId()));
        locks.lockAll(userIds);
        reserver.release(meeting);
        meetings.delete(meeting);
        metrics.meetingCancelled();
        log.info("Cancelled meeting {}", meetingId);
    }

    private List<UUID> normalizeParticipants(UUID organizerId, List<UUID> participantIds) {
        LinkedHashSet<UUID> unique = new LinkedHashSet<>();
        for (UUID participantId : participantIds) {
            if (participantId.equals(organizerId)) {
                throw new SchedulingException(
                        ErrorCode.INVALID_PARTICIPANTS,
                        "The organizer is already included and cannot be listed as a participant.");
            }
            unique.add(participantId);
        }
        if (unique.size() > limits.maxParticipants()) {
            throw new SchedulingException(
                    ErrorCode.INVALID_PARTICIPANTS,
                    "A meeting can include at most %d participants.".formatted(limits.maxParticipants()));
        }
        return List.copyOf(unique);
    }

    private Map<UUID, Calendar> index(List<Calendar> calendars) {
        Map<UUID, Calendar> byOwner = new HashMap<>();
        for (Calendar calendar : calendars) {
            byOwner.put(calendar.getOwner().getId(), calendar);
        }
        return byOwner;
    }

    private UUID organizerSlotId(UUID meetingId) {
        return organizerSlotIds(List.of(meetingId)).get(meetingId);
    }

    private Map<UUID, UUID> organizerSlotIds(List<UUID> meetingIds) {
        if (meetingIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> refs = new HashMap<>();
        for (OrganizerSlotRef ref : slots.findOrganizerSlots(meetingIds)) {
            refs.put(ref.getMeetingId(), ref.getSlotId());
        }
        return refs;
    }

    private MeetingView toView(Meeting meeting, UUID slotId) {
        List<UUID> participantIds = meeting.getParticipants().stream()
                .map(User::getId)
                .sorted()
                .toList();
        return new MeetingView(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getStartAt(),
                meeting.getEndAt(),
                meeting.getOrganizer().getId(),
                participantIds,
                slotId);
    }
}
