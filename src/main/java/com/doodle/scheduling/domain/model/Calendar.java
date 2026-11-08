package com.doodle.scheduling.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's personal schedule. Slot mutations for one owner are serialized on this row.
 * The public API does not expose this type.
 */
@Entity
@Table(name = "calendars")
public class Calendar {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Calendar() {
    }

    public Calendar(User owner) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
