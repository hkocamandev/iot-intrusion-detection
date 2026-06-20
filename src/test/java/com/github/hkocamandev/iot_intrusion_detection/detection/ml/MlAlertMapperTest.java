package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MlAlertMapperTest {

    private final MlProperties props = new MlProperties(
            true, "http://localhost:8000", 2000, 0.5,
            Map.of(AttackType.DOS, Severity.HIGH, AttackType.RECON, Severity.MEDIUM));
    private final MlAlertMapper mapper = new MlAlertMapper(props);

    private TrafficEvent flow() {
        return new TrafficEvent(Instant.now(), "tcp-5", "tcp", "http", "DOS_SYN_Hping",
                Map.of("fwd_pkts_tot", 5000.0));
    }

    @Test
    void emitsAlertForConfidentAttack() {
        Optional<AlertEvent> alert = mapper.toAlert(flow(), new MlPrediction("DOS", 0.92));

        assertThat(alert).isPresent();
        AlertEvent a = alert.get();
        assertThat(a.detectionSource()).isEqualTo(DetectionSource.ML);
        assertThat(a.attackType()).isEqualTo(AttackType.DOS);
        assertThat(a.severity()).isEqualTo(Severity.HIGH);
        assertThat(a.score()).isEqualTo(0.92);
        assertThat(a.ruleName()).isEqualTo("ml-xgboost");
        assertThat(a.sourceId()).isEqualTo("tcp-5");
    }

    @Test
    void noAlertForNormal() {
        assertThat(mapper.toAlert(flow(), new MlPrediction("NORMAL", 0.99))).isEmpty();
    }

    @Test
    void noAlertBelowMinScore() {
        assertThat(mapper.toAlert(flow(), new MlPrediction("DOS", 0.3))).isEmpty();
    }

    @Test
    void unknownLabelFallsBackToOtherWithDefaultSeverity() {
        Optional<AlertEvent> alert = mapper.toAlert(flow(), new MlPrediction("WEIRD", 0.8));
        assertThat(alert).isPresent();
        assertThat(alert.get().attackType()).isEqualTo(AttackType.OTHER);
        assertThat(alert.get().severity()).isEqualTo(Severity.MEDIUM); // default (OTHER not in map)
    }
}
