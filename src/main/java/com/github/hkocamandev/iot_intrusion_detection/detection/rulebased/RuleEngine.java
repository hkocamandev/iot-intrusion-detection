package com.github.hkocamandev.iot_intrusion_detection.detection.rulebased;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuleEngine {

    private final RuleProperties properties;

    public RuleEngine(RuleProperties properties) {
        this.properties = properties;
    }

    /** Produces an AlertEvent for each rule whose threshold is exceeded (a flow may trigger multiple rules). */
    public List<AlertEvent> evaluate(TrafficEvent event) {
        List<AlertEvent> alerts = new ArrayList<>();
        if (properties.rules() == null) return alerts;

        for (RuleProperties.ThresholdRule rule : properties.rules()) {
            Double value = event.features() == null ? null : event.features().get(rule.feature());
            if (value == null || value <= rule.threshold()) continue;

            double score = value / rule.threshold();
            alerts.add(new AlertEvent(
                    Instant.now(), event.sourceId(), DetectionSource.RULE,
                    rule.attackType(), rule.severity(), score, rule.name(),
                    event.protocol(), event.service(), event.trueLabel()));
        }
        return alerts;
    }
}
