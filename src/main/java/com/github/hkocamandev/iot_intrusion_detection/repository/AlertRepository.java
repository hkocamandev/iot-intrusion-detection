package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findTop100ByOrderByCreatedAtDesc();

    List<Alert> findTop100BySeverityOrderByCreatedAtDesc(Severity severity);

    List<Alert> findByLlmEnrichedAtIsNullOrderByCreatedAtAsc(Pageable pageable);

    @Query("select a.attackType, count(a) from Alert a group by a.attackType")
    List<Object[]> countByAttackType();

    @Query("select a.severity, count(a) from Alert a group by a.severity")
    List<Object[]> countBySeverity();

    @Query("select a.detectionSource, count(a) from Alert a group by a.detectionSource")
    List<Object[]> countByDetectionSource();
}
