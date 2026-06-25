package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.producer.TrafficProducer;
import com.github.hkocamandev.iot_intrusion_detection.repository.NetworkFlowRepository;
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
@TestPropertySource(properties = {"app.ml.enabled=false", "app.llm.enabled=false"})
class StorageConsumerIT {

    @Autowired TrafficProducer producer;
    @Autowired NetworkFlowRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void publishedEventIsStoredInDatabase() {
        TrafficEvent event = new TrafficEvent(
                Instant.now(), "dev-9", "tcp", "http",
                "DOS_SYN_Hping", Map.of("flow_duration", 2.0));

        producer.publish(event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(repository.findAll())
                        .anyMatch(f -> "dev-9".equals(f.getSourceId())
                                && f.getTrueAttackType() == AttackType.DOS
                                && f.getFeatures().containsKey("flow_duration")));
    }
}
