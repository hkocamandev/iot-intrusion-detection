package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
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

    @Test
    void findsOnlyUnenrichedAlertsOldestFirst() {
        Alert older = repository.save(new Alert(Instant.parse("2026-06-20T10:00:00Z"),
                "src-old", DetectionSource.RULE, AttackType.DOS, Severity.HIGH, 1.0,
                "r1", "tcp", "http", null));
        Alert newer = repository.save(new Alert(Instant.parse("2026-06-20T11:00:00Z"),
                "src-new", DetectionSource.RULE, AttackType.RECON, Severity.MEDIUM, 2.0,
                "r2", "tcp", "http", null));

        // both unenriched -> oldest first, batch of 1 returns the older one
        var firstBatch = repository.findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 1));
        assertThat(firstBatch).extracting(Alert::getSourceId).containsExactly("src-old");

        // enrich the older one; now only the newer is pending
        older.applyLlmEnrichment("explained", "recommended", Instant.parse("2026-06-20T12:00:00Z"));
        repository.save(older);

        var pending = repository.findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 10));
        assertThat(pending).extracting(Alert::getSourceId).containsExactly("src-new");
        assertThat(repository.findById(older.getId()).orElseThrow().getLlmExplanation())
                .isEqualTo("explained");
    }
}
