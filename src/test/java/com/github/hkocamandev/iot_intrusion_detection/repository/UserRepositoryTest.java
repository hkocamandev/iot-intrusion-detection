package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.AppUser;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired UserRepository repository;

    @Test
    void findsSavedUserByUsername() {
        repository.save(new AppUser("alice", "$2a$hash", Role.ADMIN, Instant.now()));

        AppUser found = repository.findByUsername("alice").orElseThrow();
        assertThat(found.getUsername()).isEqualTo("alice");
        assertThat(found.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(found.getRole()).isEqualTo(Role.ADMIN);
        assertThat(found.getId()).isNotNull();
    }

    @Test
    void returnsEmptyForUnknownUsername() {
        assertThat(repository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void reportsExistenceByUsername() {
        repository.save(new AppUser("bob", "$2a$hash", Role.ANALYST, Instant.now()));

        assertThat(repository.existsByUsername("bob")).isTrue();
        assertThat(repository.existsByUsername("carol")).isFalse();
    }

    @Test
    void rejectsDuplicateUsername() {
        repository.saveAndFlush(new AppUser("dave", "$2a$hash", Role.ANALYST, Instant.now()));

        assertThatThrownBy(() ->
                repository.saveAndFlush(new AppUser("dave", "$2a$other", Role.ADMIN, Instant.now())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
