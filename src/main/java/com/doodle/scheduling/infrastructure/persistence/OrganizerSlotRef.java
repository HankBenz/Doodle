package com.doodle.scheduling.infrastructure.persistence;

import java.util.UUID;

public interface OrganizerSlotRef {

    UUID getMeetingId();

    UUID getSlotId();
}
