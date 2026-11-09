package dev.hbn.doodle.modules.meeting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingContract {
    MeetingPortable get(long id);

    MeetingPortable create(MeetingCreateRequest request);

    void delete(long id);

    Page<MeetingPortable> paged(Pageable pageable);
}
