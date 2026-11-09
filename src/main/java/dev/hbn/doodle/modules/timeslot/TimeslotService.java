package dev.hbn.doodle.modules.timeslot;

import dev.hbn.doodle.modules.meeting.MeetingRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TimeslotService implements TimeslotContract {

    private final TimeslotRepository timeslotRepository;

    private final MeetingRepository meetingRepository;

    public TimeslotService(TimeslotRepository timeslotRepository, MeetingRepository meetingRepository) {
        this.timeslotRepository = timeslotRepository;
        this.meetingRepository = meetingRepository;
    }

    private Specification<TimeslotEntity> prepareQuery(TimeslotFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.ownerId() != null) {
                predicates.add(builder.equal(root.get("ownerId"), filter.ownerId()));
            }
            if (filter.start() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("start"), filter.start()));
            }
            if (filter.finish() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("finish"), filter.finish()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<TimeslotEntity> conflictsQuery(UUID ownerId, Instant start, Instant finish) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (start == null) {
            throw new IllegalArgumentException("start is required");
        }
        if (finish == null) {
            throw new IllegalArgumentException("finish is required");
        }

        return (root, query, builder) -> {
            Predicate ownerMatches = builder.equal(root.get("ownerId"), ownerId);
            Predicate startIsWithinRange = builder.between(root.get("start"), start, finish);
            Predicate finishIsWithinRange = builder.between(root.get("finish"), start, finish);
            Predicate filterStartIsWithinTimeslot = builder.and(builder.lessThanOrEqualTo(root.get("start"), start), builder.greaterThanOrEqualTo(root.get("finish"), start));
            Predicate filterFinishIsWithinTimeslot = builder.and(builder.lessThanOrEqualTo(root.get("start"), finish), builder.greaterThanOrEqualTo(root.get("finish"), finish));
            return builder.and(
                    ownerMatches,
                    builder.or(
                            builder.or(startIsWithinRange, finishIsWithinRange),
                            builder.or(filterStartIsWithinTimeslot, filterFinishIsWithinTimeslot)
                    )
            );
        };
    }

    @Override
    public TimeslotPortable get(long id) {
        var found = timeslotRepository.findById(id);
        if (found.isPresent()) {
            return TimeslotPortable.create(found.get());
        } else {
            throw new EntityNotFoundException("Timeslot with id " + id + " not found");
        }
    }

    @Override
    public TimeslotPortable create(TimeslotCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        if (!request.isValid()) {
            throw new IllegalArgumentException("request");
        }
        var count = timeslotRepository.count(conflictsQuery(request.ownerId(), request.start(), request.finish()));
        if (count > 0) {
            throw new IllegalArgumentException("timeslot has conflicts with %s other timeslots".formatted(count));
        }
        TimeslotEntity entity = new TimeslotEntity();
        entity.setStart(request.start());
        entity.setFinish(request.finish());
        entity.setStatus(request.status());
        entity.setOwnerId(request.ownerId());
        timeslotRepository.save(entity);
        return TimeslotPortable.create(entity);
    }

    @Override
    public TimeslotPortable update(TimeslotUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        if (!request.isValid()) {
            throw new IllegalArgumentException("request");
        }
        var possibleEntity = timeslotRepository.findById(request.id());
        if (possibleEntity.isEmpty()) {
            throw new EntityNotFoundException("Timeslot with id " + request.id() + " not found");
        }

        var currents = timeslotRepository.findAll(conflictsQuery(request.ownerId(), request.start(), request.finish()), Pageable.ofSize(2));
        if (currents.getNumberOfElements() > 1) {
            throw new IllegalArgumentException("timeslot has conflict with timeslot id ");
        }
        if (currents.getNumberOfElements() == 1 && currents.getContent().get(0).getId() != request.id()) {
            throw new IllegalArgumentException("timeslot has conflict with timeslot id %s".formatted(currents.getContent().get(0).getId()));
        }

        var entity = possibleEntity.get();
        entity.setStart(request.start());
        entity.setFinish(request.finish());
        entity.setStatus(request.status());
        entity.setOwnerId(request.ownerId());
        timeslotRepository.saveAndFlush(entity);
        return TimeslotPortable.create(entity);
    }

    @Override
    public void delete(long id) {
        long count = meetingRepository.countByTimeslotId(id);
        if (count > 0) {
            throw new IllegalArgumentException();
        }
        timeslotRepository.deleteById(id);
    }

    @Override
    public Page<TimeslotPortable> paged(TimeslotFilter filter, Pageable pageable) {
        return timeslotRepository.findAll(prepareQuery(filter), pageable)
                .map(TimeslotPortable::create);
    }
}
