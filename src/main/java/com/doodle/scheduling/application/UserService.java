package com.doodle.scheduling.application;

import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import com.doodle.scheduling.application.view.PageView;
import com.doodle.scheduling.application.view.UserView;
import com.doodle.scheduling.domain.model.Calendar;
import com.doodle.scheduling.domain.model.User;
import com.doodle.scheduling.infrastructure.persistence.CalendarRepository;
import com.doodle.scheduling.infrastructure.persistence.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final CalendarRepository calendars;

    public UserService(UserRepository users, CalendarRepository calendars) {
        this.users = users;
        this.calendars = calendars;
    }

    @Transactional
    public UserView create(String name, String email) {
        String normalizedName = name.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) {
            throw new SchedulingException(ErrorCode.EMAIL_TAKEN, "A user with this email already exists.");
        }
        User user = new User(normalizedName, normalizedEmail);
        users.save(user);
        calendars.save(new Calendar(user));
        return UserView.from(user);
    }

    @Transactional(readOnly = true)
    public UserView get(UUID userId) {
        return users.findById(userId)
                .map(UserView::from)
                .orElseThrow(() -> new SchedulingException(
                        ErrorCode.USER_NOT_FOUND, "User %s does not exist.".formatted(userId)));
    }

    @Transactional(readOnly = true)
    public PageView<UserView> list(int page, int size) {
        Page<UserView> result = users.findAll(PageRequests.of(page, size, Sort.by("createdAt").and(Sort.by("id"))))
                .map(UserView::from);
        return PageView.of(result);
    }
}
