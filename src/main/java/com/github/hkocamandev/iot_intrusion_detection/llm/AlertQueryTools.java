package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tools the chat assistant can call to answer questions about stored alerts. */
@Component
public class AlertQueryTools {

    private final AlertRepository repository;

    public AlertQueryTools(AlertRepository repository) {
        this.repository = repository;
    }

    @Tool("Returns the most recent alerts, newest first, up to the given limit (capped at 100).")
    public List<String> recentAlerts(int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
                .limit(capped)
                .map(a -> "%s | %s | %s | score=%.2f | rule=%s".formatted(
                        a.getCreatedAt(), a.getAttackType(), a.getSeverity(),
                        a.getScore(), a.getRuleName()))
                .toList();
    }

    @Tool("Returns the count of alerts grouped by attack type.")
    public Map<String, Long> countByAttackType() {
        return toMap(repository.countByAttackType());
    }

    @Tool("Returns the count of alerts grouped by severity.")
    public Map<String, Long> countBySeverity() {
        return toMap(repository.countBySeverity());
    }

    @Tool("Returns the count of alerts grouped by detection source (RULE or ML).")
    public Map<String, Long> countByDetectionSource() {
        return toMap(repository.countByDetectionSource());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return result;
    }
}
