package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.LlmProperties;
import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Periodically enriches unenriched alerts in bounded batches. app.llm.enabled=true ise aktif. */
@Component
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class AlertEnrichmentScheduler {

    private final AlertRepository repository;
    private final SecurityAnalystService analystService;
    private final int batchSize;

    public AlertEnrichmentScheduler(AlertRepository repository,
                                    SecurityAnalystService analystService,
                                    LlmProperties properties) {
        this.repository = repository;
        this.analystService = analystService;
        this.batchSize = properties.enrichment().batchSize();
    }

    @Scheduled(fixedDelayString = "${app.llm.enrichment.poll-interval}")
    public void enrichPending() {
        List<Alert> pending = repository.findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(
                PageRequest.of(0, batchSize));
        for (Alert alert : pending) {
            analystService.enrich(alert).ifPresent(e -> {
                alert.applyLlmEnrichment(e.explanation(), e.recommendation(), Instant.now());
                repository.save(alert);
            });
        }
    }
}
