package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.LlmProperties;
import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertEnrichmentSchedulerTest {

    private final AlertRepository repository = mock(AlertRepository.class);
    private final SecurityAnalystService analystService = mock(SecurityAnalystService.class);
    private final LlmProperties props = new LlmProperties(
            true, "anthropic", Duration.ofSeconds(30),
            new LlmProperties.Enrichment(5, Duration.ofSeconds(10)),
            new LlmProperties.Anthropic("claude-haiku-4-5"),
            new LlmProperties.Gemini("gemini-2.0-flash"));
    private final AlertEnrichmentScheduler scheduler =
            new AlertEnrichmentScheduler(repository, analystService, props);

    private Alert alert(String src) {
        return new Alert(Instant.now(), src, DetectionSource.ML, AttackType.DOS,
                Severity.HIGH, 0.9, "ml-xgboost", "tcp", "http", null);
    }

    @Test
    void enrichesAndSavesPendingAlerts() {
        Alert a = alert("src-1");
        when(repository.findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(a));
        when(analystService.enrich(a)).thenReturn(Optional.of(new Enrichment("exp", "rec")));

        scheduler.enrichPending();

        verify(repository).save(a);
        org.assertj.core.api.Assertions.assertThat(a.getLlmExplanation()).isEqualTo("exp");
        org.assertj.core.api.Assertions.assertThat(a.getLlmEnrichedAt()).isNotNull();
    }

    @Test
    void skipsAlertWhenEnrichmentEmptyAndDoesNotSave() {
        Alert a = alert("src-2");
        when(repository.findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(a));
        when(analystService.enrich(a)).thenReturn(Optional.empty());

        scheduler.enrichPending();

        verify(repository, never()).save(any());
    }
}
