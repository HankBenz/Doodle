package com.doodle.scheduling.application.error;

public enum ErrorCode {
    USER_NOT_FOUND(404),
    SLOT_NOT_FOUND(404),
    MEETING_NOT_FOUND(404),
    EMAIL_TAKEN(409),
    SLOT_OVERLAP(409),
    SLOT_LOCKED(409),
    SLOT_NOT_FREE(409),
    PARTICIPANT_BUSY(409),
    PARTICIPANT_NOT_AVAILABLE(409),
    NOT_ORGANIZER(403),
    LOCK_TIMEOUT(409),
    VERSION_CONFLICT(409),
    INVALID_DURATION(400),
    INVALID_WINDOW(400),
    INVALID_PARTICIPANTS(400),
    INVALID_REQUEST(400);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
