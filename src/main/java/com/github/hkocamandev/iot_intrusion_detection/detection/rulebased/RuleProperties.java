package com.github.hkocamandev.iot_intrusion_detection.detection.rulebased;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** app.rules altındaki eşik kurallarının config bağlaması. */
@ConfigurationProperties(prefix = "app")
public record RuleProperties(List<ThresholdRule> rules) {

    /** Tek bir eşik kuralı: belirtilen feature değeri threshold'u aşarsa alarm üretir. */
    public record ThresholdRule(
            String name,
            String feature,
            double threshold,
            AttackType attackType,
            Severity severity
    ) { }
}
