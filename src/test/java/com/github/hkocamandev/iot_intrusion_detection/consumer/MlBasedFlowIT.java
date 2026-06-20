package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.producer.TrafficProducer;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MlBasedFlowIT {

    static final MockWebServer mlServer = new MockWebServer();

    static {
        try {
            mlServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    if ("/predict".equals(request.getPath())) {
                        return new MockResponse()
                                .setHeader("Content-Type", "application/json")
                                .setBody("{\"attack_type\":\"DOS\",\"score\":0.92}");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
            mlServer.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void mlProps(DynamicPropertyRegistry registry) {
        registry.add("app.ml.enabled", () -> "true");
        registry.add("app.ml.url", () -> mlServer.url("/").toString().replaceAll("/$", ""));
    }

    @AfterAll
    static void stop() throws IOException {
        mlServer.shutdown();
    }

    @Autowired TrafficProducer trafficProducer;
    @Autowired AlertRepository alertRepository;

    @BeforeEach
    void clear() {
        alertRepository.deleteAll();
    }

    @Test
    void mlClassificationProducesMlAlert() {
        TrafficEvent event = new TrafficEvent(Instant.now(), "ml-1", "tcp", "http",
                "DOS_SYN_Hping", Map.of("fwd_pkts_tot", 5000.0));

        trafficProducer.publish(event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(alertRepository.findAll())
                        .anyMatch(a -> "ml-1".equals(a.getSourceId())
                                && a.getDetectionSource() == DetectionSource.ML
                                && a.getAttackType() == AttackType.DOS));
    }
}
