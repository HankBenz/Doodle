package com.doodle.scheduling.application.error;

public class SchedulingException extends RuntimeException {

    private final ErrorCode code;

    public SchedulingException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
