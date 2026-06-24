package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.StatsResponse;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** alerts tablosundan aggregate sayıları toplayıp StatsResponse kurar. */
@Service
public class AlertStatsService {

    private final AlertRepository repository;

    public AlertStatsService(AlertRepository repository) {
        this.repository = repository;
    }

    public StatsResponse buildStats() {
        return new StatsResponse(
                repository.count(),
                toMap(repository.countByAttackType()),
                toMap(repository.countBySeverity()),
                toMap(repository.countByDetectionSource()));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return counts;
    }
}
