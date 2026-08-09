package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Creates the initial ADMIN account, which registration cannot produce. Runs only when no
 * ADMIN account exists yet, so it never resets an existing password — and, unlike a guard on
 * "any account exists", it still seeds an ADMIN even after other roles have self-registered.
 */
@Component
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;

    public UserSeeder(UserRepository users, PasswordEncoder passwordEncoder,
                      SeedProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = properties.adminPassword();
        if (password == null || password.isBlank()) {
            log.info("Admin seeding skipped: app.security.seed.admin-password is not set");
            return;
        }
        if (users.existsByRole(Role.ADMIN)) {
            log.info("Admin seeding skipped: an ADMIN account already exists");
            return;
        }
        String username = properties.adminUsername();
        if (users.existsByUsername(username)) {
            log.error("Admin seeding failed: configured seed username '{}' is already taken by "
                    + "a non-admin account; no ADMIN account exists. Choose a different "
                    + "app.security.seed.admin-username or resolve the conflict manually.",
                    username);
            return;
        }
        users.save(new AppUser(username, passwordEncoder.encode(password), Role.ADMIN,
                Instant.now()));
        log.info("Seeded initial ADMIN account '{}'", username);
    }
}
