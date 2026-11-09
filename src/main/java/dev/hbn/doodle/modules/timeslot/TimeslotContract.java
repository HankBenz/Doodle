package dev.hbn.doodle.modules.timeslot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TimeslotContract {
    TimeslotPortable get(long id);

    TimeslotPortable create(TimeslotCreateRequest request);

    TimeslotPortable update(TimeslotUpdateRequest request);

    void delete(long id);

    Page<TimeslotPortable> paged(TimeslotFilter filter, Pageable pageable);
}
