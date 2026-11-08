package com.doodle.scheduling.domain.model;

public enum SlotRole {
    /** Created by a user as free or personally busy time. */
    USER,
    /** Busy time carved out of free slots while a meeting is active. */
    MEETING_HOLD
}
