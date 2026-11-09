package dev.hbn.doodle.modules.timeslot;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeslots")
@Getter
@Setter
public class TimeslotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private Instant start;

    @Column(nullable = false)
    private Instant finish;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeslotStatus status;
}
