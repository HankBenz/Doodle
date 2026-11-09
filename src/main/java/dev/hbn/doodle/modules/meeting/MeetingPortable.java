package dev.hbn.doodle.modules.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingPortable(
        long id,
        UUID ownerId,
        Instant start,
        Instant finish,
        String title,
        String description,
        List<UUID> participants
) {
    public static MeetingPortable create(MeetingEntity meeting) {
        return new MeetingPortable(
                meeting.getId(),
                meeting.getTimeslot().getOwnerId(),
                meeting.getTimeslot().getStart(),
                meeting.getTimeslot().getFinish(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getParticipants()
        );
    }
}
