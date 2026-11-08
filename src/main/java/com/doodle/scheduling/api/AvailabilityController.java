package com.doodle.scheduling.api;

import com.doodle.scheduling.application.AvailabilityService;
import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.application.view.AvailabilityView;
import com.doodle.scheduling.application.view.CommonAvailabilityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Availability")
public class AvailabilityController {

    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/api/v1/users/{userId}/availability")
    @Operation(summary = "Aggregated free and busy blocks for one user")
    public AvailabilityView forUser(
            @PathVariable UUID userId,
            @RequestParam(name = "from") Instant from,
            @RequestParam(name = "to") Instant to) {
        return availability.forUser(userId, from, to);
    }

    @GetMapping("/api/v1/availability")
    @Operation(summary = "Time where every listed user has a free slot")
    public CommonAvailabilityView common(
            @RequestParam(name = "userIds") String userIds,
            @RequestParam(name = "from") Instant from,
            @RequestParam(name = "to") Instant to) {
        return availability.common(parseUserIds(userIds), from, to);
    }

    private static List<UUID> parseUserIds(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new SchedulingException(ErrorCode.INVALID_REQUEST, "At least one user id is required.");
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(UUID::fromString)
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw new SchedulingException(
                    ErrorCode.INVALID_REQUEST, "userIds must be a comma-separated list of uuids.");
        }
    }
}
