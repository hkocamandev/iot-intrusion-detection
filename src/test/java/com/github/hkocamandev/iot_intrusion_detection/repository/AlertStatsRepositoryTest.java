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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AlertStatsRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired AlertRepository repository;

    private Alert alert(Instant t, AttackType type, Severity sev, DetectionSource src) {
        return new Alert(t, "s", src, type, sev, 1.0, "r", "tcp", "http", "x");
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(((Enum<?>) r[0]).name(), ((Number) r[1]).longValue());
        return m;
    }

    @Test
    void recentReturnsNewestFirstAndSeverityFilters() {
        Instant base = Instant.parse("2026-06-20T10:00:00Z");
        repository.save(alert(base, AttackType.DOS, Severity.HIGH, DetectionSource.RULE));
        repository.save(alert(base.plusSeconds(60), AttackType.RECON, Severity.MEDIUM, DetectionSource.ML));

        List<Alert> recent = repository.findTop100ByOrderByCreatedAtDesc();
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getAttackType()).isEqualTo(AttackType.RECON); // newest first

        List<Alert> high = repository.findTop100BySeverityOrderByCreatedAtDesc(Severity.HIGH);
        assertThat(high).hasSize(1);
        assertThat(high.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void aggregateCountsGroupCorrectly() {
        Instant base = Instant.parse("2026-06-20T10:00:00Z");
        repository.save(alert(base, AttackType.DOS, Severity.HIGH, DetectionSource.RULE));
        repository.save(alert(base, AttackType.DOS, Severity.HIGH, DetectionSource.ML));
        repository.save(alert(base, AttackType.RECON, Severity.MEDIUM, DetectionSource.RULE));

        assertThat(toMap(repository.countByAttackType()))
                .containsEntry("DOS", 2L).containsEntry("RECON", 1L);
        assertThat(toMap(repository.countBySeverity()))
                .containsEntry("HIGH", 2L).containsEntry("MEDIUM", 1L);
        assertThat(toMap(repository.countByDetectionSource()))
                .containsEntry("RULE", 2L).containsEntry("ML", 1L);
    }
}
