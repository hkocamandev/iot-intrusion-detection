package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.producer.TrafficProducer;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import com.github.hkocamandev.iot_intrusion_detection.repository.NetworkFlowRepository;
import okhttp3.mockwebserver.Dispatcher;
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
class MlFallbackIT {

    static final MockWebServer mlServer = new MockWebServer();

    static {
        try {
            mlServer.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    return new MockResponse().setResponseCode(503);
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
    @Autowired NetworkFlowRepository networkFlowRepository;

    @BeforeEach
    void clear() {
        alertRepository.deleteAll();
    }

    @Test
    void mlDownYieldsNoAlertButFlowIsStored() {
        // benign flow: below all rule thresholds, and ml-service returns 503
        TrafficEvent event = new TrafficEvent(Instant.now(), "ml-down-1", "tcp", "http",
                "Normal", Map.of("fwd_pkts_tot", 5.0));

        trafficProducer.publish(event);

        // storage consumer still persists the flow (ML failure does not affect other groups)
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(networkFlowRepository.findAll())
                        .anyMatch(f -> "ml-down-1".equals(f.getSourceId())));

        // no alert: no rule fired and the ML call fell back silently
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                assertThat(alertRepository.findAll())
                        .noneMatch(a -> "ml-down-1".equals(a.getSourceId())));
    }
}
