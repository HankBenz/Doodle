package com.doodle.scheduling.infrastructure.persistence;

import com.doodle.scheduling.domain.availability.SlotSlice;
import com.doodle.scheduling.domain.model.TimeSlot;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID>, JpaSpecificationExecutor<TimeSlot> {

    Optional<TimeSlot> findByIdAndCalendar_Id(UUID id, UUID calendarId);

    @Query("""
            select s from TimeSlot s
            where s.calendar.id = :calendarId
              and s.startAt < :end
              and s.endAt > :start
            order by s.startAt
            """)
    List<TimeSlot> findOverlapping(
            @Param("calendarId") UUID calendarId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            select s.id from TimeSlot s
            where s.calendar.id = :calendarId
              and s.startAt < :end
              and s.endAt > :start
              and s.id <> :excludeId
            """)
    List<UUID> findOverlappingIds(
            @Param("calendarId") UUID calendarId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") UUID excludeId,
            Pageable pageable);

    @Query("""
            select new com.doodle.scheduling.domain.availability.SlotSlice(
                s.id, s.startAt, s.endAt, s.status)
            from TimeSlot s
            where s.calendar.id = :calendarId
              and s.startAt < :to
              and s.endAt > :from
            order by s.startAt
            """)
    List<SlotSlice> findSlices(
            @Param("calendarId") UUID calendarId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select s from TimeSlot s
            where s.calendar.id = :calendarId
              and s.status = com.doodle.scheduling.domain.model.SlotStatus.FREE
              and s.role = com.doodle.scheduling.domain.model.SlotRole.USER
              and s.startAt <= :end
              and s.endAt >= :start
            order by s.startAt
            """)
    List<TimeSlot> findFreeTouching(
            @Param("calendarId") UUID calendarId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            select s from TimeSlot s
            join fetch s.calendar
            where s.meeting.id = :meetingId
            """)
    List<TimeSlot> findByMeetingId(@Param("meetingId") UUID meetingId);

    @Query("""
            select s.meeting.id as meetingId, s.id as slotId
            from TimeSlot s
            where s.meeting.id in :meetingIds
              and s.role = com.doodle.scheduling.domain.model.SlotRole.USER
              and s.calendar.owner = s.meeting.organizer
            """)
    List<OrganizerSlotRef> findOrganizerSlots(@Param("meetingIds") Collection<UUID> meetingIds);
}
