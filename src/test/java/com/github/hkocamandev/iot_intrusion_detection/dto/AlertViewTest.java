package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertViewTest {

    @Test
    void fromAlertEventMapsFieldsWithNullIdAndDetectedAtAsCreatedAt() {
        Instant t = Instant.parse("2026-06-20T10:00:00Z");
        AlertEvent event = new AlertEvent(t, "src-1", DetectionSource.ML, AttackType.DOS,
                Severity.HIGH, 0.92, "ml-xgboost", "tcp", "http", "DOS_SYN_Hping");

        AlertView view = AlertView.from(event);

        assertThat(view.id()).isNull();
        assertThat(view.createdAt()).isEqualTo(t);
        assertThat(view.sourceId()).isEqualTo("src-1");
        assertThat(view.detectionSource()).isEqualTo(DetectionSource.ML);
        assertThat(view.attackType()).isEqualTo(AttackType.DOS);
        assertThat(view.severity()).isEqualTo(Severity.HIGH);
        assertThat(view.score()).isEqualTo(0.92);
        assertThat(view.ruleName()).isEqualTo("ml-xgboost");
    }

    @Test
    void fromAlertMapsEntityFields() {
        Instant t = Instant.parse("2026-06-20T11:00:00Z");
        Alert alert = new Alert(t, "src-2", DetectionSource.RULE, AttackType.RECON,
                Severity.MEDIUM, 2.0, "recon-long-flow", "tcp", "http", "Recon_Nmap");

        AlertView view = AlertView.from(alert);

        assertThat(view.createdAt()).isEqualTo(t);
        assertThat(view.sourceId()).isEqualTo("src-2");
        assertThat(view.detectionSource()).isEqualTo(DetectionSource.RULE);
        assertThat(view.attackType()).isEqualTo(AttackType.RECON);
        assertThat(view.severity()).isEqualTo(Severity.MEDIUM);
        assertThat(view.ruleName()).isEqualTo("recon-long-flow");
        assertThat(view.protocol()).isEqualTo("tcp");
        assertThat(view.service()).isEqualTo("http");
    }
}
