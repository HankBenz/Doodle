package com.doodle.scheduling.domain.availability;

import java.time.Instant;

public record TimeWindow(Instant start, Instant end) {
}
