package com.doodle.scheduling.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SlotRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TimeSlot() {
    }

    public static TimeSlot userSlot(Calendar calendar, Instant start, Instant end, SlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.id = UUID.randomUUID();
        slot.calendar = calendar;
        slot.startAt = start;
        slot.endAt = end;
        slot.status = status;
        slot.role = SlotRole.USER;
        slot.touch();
        return slot;
    }

    public static TimeSlot meetingHold(Calendar calendar, Instant start, Instant end, Meeting meeting) {
        TimeSlot slot = new TimeSlot();
        slot.id = UUID.randomUUID();
        slot.calendar = calendar;
        slot.startAt = start;
        slot.endAt = end;
        slot.status = SlotStatus.BUSY;
        slot.role = SlotRole.MEETING_HOLD;
        slot.meeting = meeting;
        slot.touch();
        return slot;
    }

    public void changeRange(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Slot start must be before end.");
        }
        this.startAt = start;
        this.endAt = end;
    }

    public void changeStatus(SlotStatus status) {
        this.status = status;
    }

    public void attachMeeting(Meeting meeting) {
        if (role != SlotRole.USER || status != SlotStatus.FREE || this.meeting != null) {
            throw new IllegalStateException("Only a free user slot can be booked.");
        }
        this.status = SlotStatus.BUSY;
        this.meeting = meeting;
    }

    public void releaseFromMeeting() {
        if (role != SlotRole.USER || meeting == null) {
            throw new IllegalStateException("Slot is not a booked user slot.");
        }
        this.status = SlotStatus.FREE;
        this.meeting = null;
    }

    public void convertHoldToFree() {
        if (role != SlotRole.MEETING_HOLD || meeting == null) {
            throw new IllegalStateException("Slot is not a meeting hold.");
        }
        this.role = SlotRole.USER;
        this.status = SlotStatus.FREE;
        this.meeting = null;
    }

    public UUID linkedMeetingId() {
        return meeting == null ? null : meeting.getId();
    }

    public UUID getId() {
        return id;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public SlotRole getRole() {
        return role;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        touch();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private void touch() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
