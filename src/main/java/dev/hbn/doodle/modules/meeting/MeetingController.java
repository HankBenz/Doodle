package dev.hbn.doodle.modules.meeting;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("meetings")
@RestController
public class MeetingController {
    private final MeetingContract meetingProvider;

    public MeetingController(MeetingContract meetingProvider) {
        this.meetingProvider = meetingProvider;
    }

    @GetMapping
    List<MeetingPortable> meetings(@ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
        return meetingProvider.paged(pageable);
    }

    @PostMapping
    MeetingPortable create(@Valid @RequestBody MeetingCreateRequest request) {
        return meetingProvider.create(request);
    }

    @DeleteMapping("{id}")
    void delete(@PathVariable("id") long id) {
        meetingProvider.delete(id);
    }
}
