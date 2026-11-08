package com.doodle.scheduling.infrastructure.persistence;

import com.doodle.scheduling.domain.model.SlotStatus;
import com.doodle.scheduling.domain.model.TimeSlot;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TimeSlotSpecifications {

    private TimeSlotSpecifications() {
    }

    public static Specification<TimeSlot> belongingTo(UUID calendarId, Instant from, Instant to, SlotStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("calendar").get("id"), calendarId));
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThan(root.get("endAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("startAt"), to));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
