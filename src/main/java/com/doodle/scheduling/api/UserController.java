package com.doodle.scheduling.api;

import com.doodle.scheduling.api.dto.CreateUserRequest;
import com.doodle.scheduling.application.UserService;
import com.doodle.scheduling.application.view.PageView;
import com.doodle.scheduling.application.view.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    @Operation(summary = "Create a user and their private schedule")
    public ResponseEntity<UserView> create(@Valid @RequestBody CreateUserRequest request) {
        UserView user = users.create(request.name(), request.email());
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    public UserView get(@PathVariable UUID userId) {
        return users.get(userId);
    }

    @GetMapping
    @Operation(summary = "List users")
    public PageView<UserView> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return users.list(page, size);
    }
}
