package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Enriches an alert via the SecurityAnalyst AiService. Never throws — returns empty on failure. */
@Service
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class SecurityAnalystService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAnalystService.class);

    private final SecurityAnalyst analyst;

    public SecurityAnalystService(SecurityAnalyst analyst) {
        this.analyst = analyst;
    }

    public Optional<Enrichment> enrich(Alert alert) {
        try {
            String summary = """
                    Attack type: %s
                    Severity: %s
                    Score: %.3f
                    Protocol: %s
                    Service: %s
                    Rule: %s
                    Source id: %s""".formatted(
                    alert.getAttackType(), alert.getSeverity(), alert.getScore(),
                    alert.getProtocol(), alert.getService(), alert.getRuleName(), alert.getSourceId());
            return Optional.ofNullable(analyst.analyze(summary));
        } catch (Exception e) {
            log.warn("LLM enrichment failed for source {}: {}", alert.getSourceId(), e.toString());
            return Optional.empty();
        }
    }
}
