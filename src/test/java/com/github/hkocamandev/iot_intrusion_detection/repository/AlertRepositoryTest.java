package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.*;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AlertRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired AlertRepository repository;

    @Test
    void savesAndReadsAlert() {
        Alert alert = new Alert(Instant.now(), "tcp-3", DetectionSource.RULE,
                AttackType.DOS, Severity.HIGH, 2.5, "dos-high-packet-rate",
                "tcp", "http", "DOS_SYN_Hping");

        Alert saved = repository.save(alert);

        Alert found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getDetectionSource()).isEqualTo(DetectionSource.RULE);
        assertThat(found.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(found.getLlmExplanation()).isNull();
    }
}
