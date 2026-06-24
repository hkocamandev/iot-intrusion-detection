package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.StatsResponse;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertStatsServiceTest {

    private final AlertRepository repository = mock(AlertRepository.class);
    private final AlertStatsService service = new AlertStatsService(repository);

    @Test
    void buildsStatsFromAggregateRows() {
        when(repository.count()).thenReturn(3L);
        when(repository.countByAttackType()).thenReturn(List.of(
                new Object[]{AttackType.DOS, 2L}, new Object[]{AttackType.RECON, 1L}));
        when(repository.countBySeverity()).thenReturn(List.of(
                new Object[]{Severity.HIGH, 2L}, new Object[]{Severity.MEDIUM, 1L}));
        when(repository.countByDetectionSource()).thenReturn(List.of(
                new Object[]{DetectionSource.RULE, 2L}, new Object[]{DetectionSource.ML, 1L}));

        StatsResponse stats = service.buildStats();

        assertThat(stats.total()).isEqualTo(3L);
        assertThat(stats.byAttackType()).containsEntry("DOS", 2L).containsEntry("RECON", 1L);
        assertThat(stats.bySeverity()).containsEntry("HIGH", 2L).containsEntry("MEDIUM", 1L);
        assertThat(stats.byDetectionSource()).containsEntry("RULE", 2L).containsEntry("ML", 1L);
    }
}
