package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityAnalystServiceTest {

    private final SecurityAnalyst analyst = mock(SecurityAnalyst.class);
    private final SecurityAnalystService service = new SecurityAnalystService(analyst);

    private Alert sampleAlert() {
        return new Alert(Instant.now(), "src-1", DetectionSource.ML, AttackType.DOS,
                Severity.HIGH, 0.92, "ml-xgboost", "tcp", "http", null);
    }

    @Test
    void mapsModelResponseToEnrichment() {
        when(analyst.analyze(anyString())).thenReturn(new Enrichment("explained", "do this"));

        Optional<Enrichment> result = service.enrich(sampleAlert());

        assertThat(result).contains(new Enrichment("explained", "do this"));
    }

    @Test
    void returnsEmptyWhenModelFails() {
        when(analyst.analyze(anyString())).thenThrow(new RuntimeException("api down"));

        assertThat(service.enrich(sampleAlert())).isEmpty();
    }
}
