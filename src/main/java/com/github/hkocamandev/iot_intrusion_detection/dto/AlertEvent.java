package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;

import java.time.Instant;

/** Kafka iot.alerts message. An alert produced by a rule (or, later, by ML). */
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
