package com.github.hkocamandev.iot_intrusion_detection.dto;

import java.util.Map;

/** /api/stats response: total + counts by type/severity/source (enum name -> count). */
public record StatsResponse(
        long total,
        Map<String, Long> byAttackType,
        Map<String, Long> bySeverity,
        Map<String, Long> byDetectionSource
) { }
