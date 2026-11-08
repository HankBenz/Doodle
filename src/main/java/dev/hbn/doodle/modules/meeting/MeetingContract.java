package dev.hbn.doodle.modules.meeting;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MeetingContract {
    MeetingPortable get(long id);
    MeetingPortable create(MeetingCreateRequest request);
    void delete(long id);
    List<MeetingPortable> paged(Pageable pageable);
}
