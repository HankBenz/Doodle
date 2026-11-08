package dev.hbn.doodle.modules.timeslot;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TimeslotContract {
    TimeslotPortable get(long id);
    TimeslotPortable create(TimeslotCreateRequest request);
    TimeslotPortable update(TimeslotUpdateRequest request);
    void delete(long id);
    void delete(long[] ids);
    List<TimeslotPortable> paged(TimeslotFilter filter, Pageable pageable);
    TimeslotPortable schedule(TimeslotScheduleRequest request);
}
