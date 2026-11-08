package dev.hbn.doodle.modules.timeslot;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class TimeslotService implements TimeslotContract {

    private final TimeslotRepository timeslotRepository;

    public TimeslotService(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    public Specification<TimeslotEntity> prepareQuery(TimeslotFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.ownerId() != null) {
                predicates.add(builder.equal(root.get("ownerId"), filter.ownerId()));
            }
            if (filter.begin() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("start"), filter.begin()));
            }
            if (filter.end() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("finish"), filter.end()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
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
        TimeslotFilter timeslotFilter = new TimeslotFilter(request.ownerId(), request.start(), request.finish(), null);
        var count = timeslotRepository.count(prepareQuery(timeslotFilter));
        if (count > 0) {
            throw new IllegalArgumentException("timeslot already exists");
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

        TimeslotFilter timeslotFilter = new TimeslotFilter(request.ownerId(), request.start(), request.finish(), null);
        var current = timeslotRepository.findAll(prepareQuery(timeslotFilter), Pageable.ofSize(2));
        if (current.getSize() > 1) {
            throw new IllegalArgumentException("timeslot has conflict with timeslot id ");
        }
        if (current.getSize() == 1 && current.getContent().get(0).getId() != request.id()) {
            throw new IllegalArgumentException("timeslot has conflict with timeslot id ");
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
        timeslotRepository.deleteById(id);
    }

    @Override
    public void delete(long[] ids) {
        timeslotRepository.deleteAllById(Arrays.stream(ids).boxed().toList());
    }

    @Override
    public List<TimeslotPortable> paged(TimeslotFilter filter, Pageable pageable) {
        return timeslotRepository.findAll(prepareQuery(filter), pageable)
                .map(TimeslotPortable::create)
                .toList();
    }

    @Override
    public TimeslotPortable schedule(TimeslotScheduleRequest request) {
        return null;
    }
}
