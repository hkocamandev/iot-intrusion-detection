package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;

import java.time.Instant;

/** Read-only alert view for REST + WebSocket. Buildable from an Alert entity (id set) or
 *  from an AlertEvent (id=null, createdAt=detectedAt). */
public record AlertView(
        Long id,
        Instant createdAt,
        String sourceId,
        DetectionSource detectionSource,
        AttackType attackType,
        Severity severity,
        double score,
        String ruleName,
        String protocol,
        String service,
        String llmExplanation,
        String llmRecommendation
) {
    public static AlertView from(Alert a) {
        return new AlertView(a.getId(), a.getCreatedAt(), a.getSourceId(), a.getDetectionSource(),
                a.getAttackType(), a.getSeverity(), a.getScore(), a.getRuleName(),
                a.getProtocol(), a.getService(), a.getLlmExplanation(), a.getLlmRecommendation());
    }

    public static AlertView from(AlertEvent e) {
        return new AlertView(null, e.detectedAt(), e.sourceId(), e.detectionSource(),
                e.attackType(), e.severity(), e.score(), e.ruleName(),
                e.protocol(), e.service(), null, null);
    }
}
