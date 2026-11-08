package dev.hbn.doodle.modules.timeslot;

import java.time.Instant;
import java.util.UUID;

public record TimeslotPortable(
        long id,
        UUID ownerId,
        Instant Start,
        Instant Finish,
        TimeSlotStatus Status
) {
    public static TimeslotPortable create(TimeslotEntity slot) {
        return new TimeslotPortable(slot.getId(), slot.getOwnerId(), slot.getStart(), slot.getFinish(), slot.getStatus());
    }
}
