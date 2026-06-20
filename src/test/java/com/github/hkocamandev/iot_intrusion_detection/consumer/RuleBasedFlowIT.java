package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.producer.TrafficProducer;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.ml.enabled=false")
class RuleBasedFlowIT {

    @Autowired TrafficProducer trafficProducer;
    @Autowired AlertRepository alertRepository;

    @BeforeEach
    void clear() { alertRepository.deleteAll(); }

    @Test
    void maliciousFlowProducesPersistedAlert() {
        // fwd_pkts_tot=5000 > dos-high-packet-rate threshold (1000)
        TrafficEvent event = new TrafficEvent(Instant.now(), "tcp-7", "tcp", "http",
                "DOS_SYN_Hping", Map.of("fwd_pkts_tot", 5000.0));

        trafficProducer.publish(event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(alertRepository.findAll())
                        .anyMatch(a -> "tcp-7".equals(a.getSourceId())
                                && a.getAttackType() == AttackType.DOS
                                && a.getSeverity() == Severity.HIGH));
    }

    @Test
    void benignFlowProducesNoAlert() {
        TrafficEvent event = new TrafficEvent(Instant.now(), "tcp-8", "tcp", "http",
                "Normal", Map.of("fwd_pkts_tot", 5.0));

        trafficProducer.publish(event);

        // short wait; no alert should be produced
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(6)).untilAsserted(() ->
                assertThat(alertRepository.findAll()).noneMatch(a -> "tcp-8".equals(a.getSourceId())));
    }
}
