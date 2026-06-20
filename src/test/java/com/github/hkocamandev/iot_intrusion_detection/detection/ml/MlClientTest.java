package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MlClientTest {

    private MockWebServer server;
    private MlClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String url = server.url("/").toString().replaceAll("/$", "");
        client = new MlClient(new MlProperties(true, url, 2000, 0.5, Map.of()));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void mapsSuccessfulPrediction() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"attack_type\":\"DOS\",\"score\":0.92}"));

        Optional<MlPrediction> result = client.predict(Map.of("fwd_pkts_tot", 5000.0));

        assertThat(result).isPresent();
        assertThat(result.get().attackType()).isEqualTo("DOS");
        assertThat(result.get().score()).isEqualTo(0.92);
    }

    @Test
    void returnsEmptyOnServerError() {
        server.enqueue(new MockResponse().setResponseCode(503));
        assertThat(client.predict(Map.of("x", 1.0))).isEmpty();
    }

    @Test
    void returnsEmptyOnMalformedBody() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("not-json"));
        assertThat(client.predict(Map.of("x", 1.0))).isEmpty();
    }
}
