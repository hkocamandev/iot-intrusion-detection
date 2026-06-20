package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** app.ml config binding. */
@ConfigurationProperties(prefix = "app.ml")
public record MlProperties(
        boolean enabled,
        String url,
        long timeoutMs,
        double minScore,
        Map<AttackType, Severity> severityByType
) { }
