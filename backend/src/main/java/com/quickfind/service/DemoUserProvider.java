package com.quickfind.service;

import com.quickfind.config.QuickFindProperties;
import com.quickfind.entity.User;
import com.quickfind.exception.ResourceNotFoundException;
import com.quickfind.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * The MVP has no login: requests without a userId act as the seeded demo user.
 * This is the single place to replace with the authenticated principal once Spring
 * Security is added.
 */
@Component
public class DemoUserProvider {

    private final UserRepository userRepository;
    private final String demoEmail;
    private volatile Long cachedId;

    public DemoUserProvider(UserRepository userRepository, QuickFindProperties properties) {
        this.userRepository = userRepository;
        this.demoEmail = properties.demoUserEmail();
    }

    public User demoUser() {
        return userRepository.findByEmail(demoEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Demo user", demoEmail));
    }

    public Long demoUserId() {
        Long id = cachedId;
        if (id == null) {
            id = demoUser().getId();
            cachedId = id;
        }
        return id;
    }

    /** The given user id, or the demo user's id when none is given. */
    public Long resolve(Long userId) {
        return userId != null ? userId : demoUserId();
    }
}
