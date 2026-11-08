package com.doodle.scheduling.application.view;

import com.doodle.scheduling.domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record UserView(UUID id, String name, String email, Instant createdAt) {

    public static UserView from(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
