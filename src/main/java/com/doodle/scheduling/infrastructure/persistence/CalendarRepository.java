package com.doodle.scheduling.infrastructure.persistence;

import com.doodle.scheduling.domain.model.Calendar;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

    Optional<Calendar> findByOwner_Id(UUID ownerId);

    @Query("select c.id from Calendar c where c.owner.id = :ownerId")
    Optional<UUID> findIdByOwnerId(@Param("ownerId") UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Calendar c where c.id = :id")
    Optional<Calendar> lockById(@Param("id") UUID id);
}
