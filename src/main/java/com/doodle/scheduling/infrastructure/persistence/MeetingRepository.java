package com.doodle.scheduling.infrastructure.persistence;

import com.doodle.scheduling.domain.model.Meeting;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meeting m where m.id = :id")
    Optional<Meeting> lockById(@Param("id") UUID id);

    @Query("""
            select m from Meeting m
            where m.organizer.id = :userId
              and m.startAt < :to
              and m.endAt > :from
            order by m.startAt asc
            """)
    List<Meeting> findOrganized(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select m from Meeting m join m.participants participant
            where participant.id = :userId
              and m.startAt < :to
              and m.endAt > :from
            order by m.startAt asc
            """)
    List<Meeting> findParticipating(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
