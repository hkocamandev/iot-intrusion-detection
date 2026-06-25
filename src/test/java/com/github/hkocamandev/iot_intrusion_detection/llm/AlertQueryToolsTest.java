package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertQueryToolsTest {

    private final AlertRepository repository = mock(AlertRepository.class);
    private final AlertQueryTools tools = new AlertQueryTools(repository);

    @Test
    void countByAttackTypeMapsEnumNameToCount() {
        when(repository.countByAttackType())
                .thenReturn(List.of(new Object[]{AttackType.DOS, 2L}, new Object[]{AttackType.RECON, 1L}));

        assertThat(tools.countByAttackType())
                .containsEntry("DOS", 2L)
                .containsEntry("RECON", 1L);
    }

    @Test
    void recentAlertsRespectsLimitAndFormatsRows() {
        Alert a = new Alert(Instant.parse("2026-06-20T10:00:00Z"), "src-1", DetectionSource.ML,
                AttackType.DOS, Severity.HIGH, 0.91, "ml-xgboost", "tcp", "http", null);
        when(repository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(a));

        List<String> rows = tools.recentAlerts(5);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).contains("DOS").contains("HIGH");
    }

    @Test
    void recentAlertsTruncatesToLimit() {
        Alert a1 = new Alert(Instant.parse("2026-06-20T10:00:00Z"), "src-1", DetectionSource.ML,
                AttackType.DOS, Severity.HIGH, 0.91, "ml-xgboost", "tcp", "http", null);
        Alert a2 = new Alert(Instant.parse("2026-06-21T10:00:00Z"), "src-2", DetectionSource.RULE,
                AttackType.RECON, Severity.MEDIUM, 0.75, "rule-1", "udp", "dns", null);
        Alert a3 = new Alert(Instant.parse("2026-06-22T10:00:00Z"), "src-3", DetectionSource.ML,
                AttackType.PORT_SCAN, Severity.LOW, 0.45, "ml-isolation-forest", "tcp", "ssh", null);
        when(repository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(a1, a2, a3));

        List<String> rows = tools.recentAlerts(2);

        assertThat(rows).hasSize(2);
    }
}
