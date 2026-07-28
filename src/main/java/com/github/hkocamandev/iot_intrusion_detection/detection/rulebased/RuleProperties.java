package com.github.hkocamandev.iot_intrusion_detection.detection.rulebased;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Config binding for the threshold rules under app.rules. */
@ConfigurationProperties(prefix = "app")
public record RuleProperties(List<ThresholdRule> rules) {

    /** A single threshold rule: raises an alert when the given feature value exceeds the threshold. */
    public record ThresholdRule(
            String name,
            String feature,
            double threshold,
            AttackType attackType,
            Severity severity
    ) { }
}
