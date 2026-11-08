package com.doodle.scheduling.api;

import com.doodle.scheduling.api.dto.CreateMeetingRequest;
import com.doodle.scheduling.application.MeetingService;
import com.doodle.scheduling.application.view.MeetingView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Meetings")
public class MeetingController {

    private final MeetingService meetings;

    public MeetingController(MeetingService meetings) {
        this.meetings = meetings;
    }

    @PostMapping("/meetings")
    @Operation(summary = "Book a free slot as a meeting")
    public ResponseEntity<MeetingView> create(@Valid @RequestBody CreateMeetingRequest request) {
        MeetingView meeting = meetings.book(
                request.organizerId(),
                request.slotId(),
                request.title(),
                request.description(),
                request.participantIds());
        return ResponseEntity.created(URI.create("/api/v1/meetings/" + meeting.id())).body(meeting);
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Get a meeting")
    public MeetingView get(@PathVariable UUID meetingId) {
        return meetings.get(meetingId);
    }

    @GetMapping("/users/{userId}/meetings")
    @Operation(summary = "List meetings a user organizes or attends in a time window")
    public List<MeetingView> list(
            @PathVariable UUID userId,
            @RequestParam(name = "from") Instant from,
            @RequestParam(name = "to") Instant to) {
        return meetings.list(userId, from, to);
    }

    @DeleteMapping("/meetings/{meetingId}")
    @Operation(summary = "Cancel a meeting and return the booked time to free")
    public ResponseEntity<Void> delete(
            @PathVariable UUID meetingId,
            @RequestParam(name = "organizerId") UUID organizerId) {
        meetings.cancel(meetingId, organizerId);
        return ResponseEntity.noContent().build();
    }
}
