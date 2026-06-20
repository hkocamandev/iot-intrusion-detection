package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** ml-service'e REST çağrısı. Hata/timeout durumunda boş döner (fallback), asla exception fırlatmaz. */
@Component
public class MlClient {

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final RestClient restClient;
    private final String url;

    public MlClient(MlProperties properties) {
        this.url = properties.url();
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<MlPrediction> predict(Map<String, Double> features) {
        try {
            MlPrediction prediction = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("features", features == null ? Map.of() : features))
                    .retrieve()
                    .body(MlPrediction.class);
            return Optional.ofNullable(prediction);
        } catch (Exception e) {
            log.warn("ML predict failed against {}: {}", url, e.toString());
            return Optional.empty();
        }
    }
}
