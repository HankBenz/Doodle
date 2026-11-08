package com.doodle.scheduling.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SchedulingMetrics {

    private final MeterRegistry registry;
    private final Counter slotsCreated;
    private final Counter slotOverlaps;
    private final Counter meetingsBooked;
    private final Counter meetingsCancelled;

    public SchedulingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.slotsCreated = Counter.builder("doodle.slots.created")
                .description("Slots created by users")
                .register(registry);
        this.slotOverlaps = Counter.builder("doodle.slots.overlap")
                .description("Slot writes rejected because the time overlaps")
                .register(registry);
        this.meetingsBooked = Counter.builder("doodle.meetings.booked")
                .description("Meetings booked")
                .register(registry);
        this.meetingsCancelled = Counter.builder("doodle.meetings.cancelled")
                .description("Meetings cancelled")
                .register(registry);
    }

    public void slotCreated() {
        slotsCreated.increment();
    }

    public void slotOverlap() {
        slotOverlaps.increment();
    }

    public void meetingBooked() {
        meetingsBooked.increment();
    }

    public void meetingCancelled() {
        meetingsCancelled.increment();
    }

    public void meetingRejected(String reason) {
        registry.counter("doodle.meetings.rejected", "reason", reason).increment();
    }

    public <T> T timeAvailability(String scope, Supplier<T> supplier) {
        return Timer.builder("doodle.availability.query")
                .description("Availability reads")
                .tag("scope", scope)
                .register(registry)
                .record(supplier);
    }
}
