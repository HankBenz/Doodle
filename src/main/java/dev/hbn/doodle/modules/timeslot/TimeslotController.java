package dev.hbn.doodle.modules.timeslot;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/timeslots")
@RestController
public class TimeslotController {
    private final TimeslotContract timeslotProvider;

    public TimeslotController(TimeslotContract timeslotProvider) {
        this.timeslotProvider = timeslotProvider;
    }

    @GetMapping
    List<TimeslotPortable> getTimeslots(TimeslotFilter filter, @ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
        return timeslotProvider.paged(filter, pageable);
    }

    @PostMapping
    TimeslotPortable create(@Valid @RequestBody TimeslotCreateRequest request) {
        return timeslotProvider.create(request);
    }

    @PutMapping
    TimeslotPortable update(@Valid @RequestBody TimeslotUpdateRequest request) {
        return timeslotProvider.update(request);
    }

    @PatchMapping
    TimeslotPortable schedule(@Valid @RequestBody TimeslotScheduleRequest request) {
        return timeslotProvider.schedule(request);
    }

    @DeleteMapping("{id}")
    void delete(@PathVariable("id") long id) {
        timeslotProvider.delete(id);
    }

    @DeleteMapping
    void delete(long[] ids) {
        timeslotProvider.delete(ids);
    }
}
