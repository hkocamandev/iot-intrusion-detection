package com.github.hkocamandev.iot_intrusion_detection.detection.rulebased;

import com.github.hkocamandev.iot_intrusion_detection.detection.rulebased.RuleProperties.ThresholdRule;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleProperties props = new RuleProperties(List.of(
            new ThresholdRule("dos-high-packet-rate", "fwd_pkts_tot", 1000, AttackType.DOS, Severity.HIGH),
            new ThresholdRule("recon-long-flow", "flow_duration", 60, AttackType.RECON, Severity.MEDIUM)));
    private final RuleEngine engine = new RuleEngine(props);

    @Test
    void emitsAlertWhenFeatureExceedsThreshold() {
        TrafficEvent event = new TrafficEvent(Instant.now(), "tcp-1", "tcp", "http",
                "DOS_SYN_Hping", Map.of("fwd_pkts_tot", 5000.0));

        List<AlertEvent> alerts = engine.evaluate(event);

        assertThat(alerts).hasSize(1);
        AlertEvent a = alerts.get(0);
        assertThat(a.attackType()).isEqualTo(AttackType.DOS);
        assertThat(a.severity()).isEqualTo(Severity.HIGH);
        assertThat(a.detectionSource()).isEqualTo(DetectionSource.RULE);
        assertThat(a.ruleName()).isEqualTo("dos-high-packet-rate");
        assertThat(a.score()).isEqualTo(5.0); // value / threshold
    }

    @Test
    void noAlertWhenBelowThresholdOrFeatureMissing() {
        TrafficEvent below = new TrafficEvent(Instant.now(), "tcp-1", "tcp", "http",
                "Normal", Map.of("fwd_pkts_tot", 10.0));
        TrafficEvent missing = new TrafficEvent(Instant.now(), "tcp-1", "tcp", "http",
                "Normal", Map.of("other", 9999.0));

        assertThat(engine.evaluate(below)).isEmpty();
        assertThat(engine.evaluate(missing)).isEmpty();
    }

    @Test
    void multipleRulesCanFireForOneFlow() {
        TrafficEvent event = new TrafficEvent(Instant.now(), "tcp-1", "tcp", "http",
                "mixed", Map.of("fwd_pkts_tot", 2000.0, "flow_duration", 120.0));

        assertThat(engine.evaluate(event)).hasSize(2);
    }
}
