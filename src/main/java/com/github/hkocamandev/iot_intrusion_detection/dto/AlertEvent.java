package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;

import java.time.Instant;

/** Kafka iot.alerts mesajı. Bir kuralın (veya ileride ML'in) ürettiği alarm. */
public record AlertEvent(
        Instant detectedAt,
        String sourceId,
        DetectionSource detectionSource,
        AttackType attackType,
        Severity severity,
        double score,
        String ruleName,
        String protocol,
        String service,
        String trueLabel
) { }
