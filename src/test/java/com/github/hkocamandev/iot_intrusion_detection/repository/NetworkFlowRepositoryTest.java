package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.NetworkFlow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class NetworkFlowRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    NetworkFlowRepository repository;

    @Test
    void savesAndReadsFlowWithJsonFeatures() {
        NetworkFlow flow = new NetworkFlow(
                Instant.now(), "dev-1", "tcp", "http",
                "DOS_SYN_Hping", AttackType.DOS, Map.of("flow_duration", 1.5, "fwd_pkts", 10.0));

        NetworkFlow saved = repository.save(flow);

        NetworkFlow found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTrueAttackType()).isEqualTo(AttackType.DOS);
        assertThat(found.getFeatures()).containsEntry("fwd_pkts", 10.0);
        assertThat(found.getFeatures()).containsEntry("flow_duration", 1.5);
    }
}
