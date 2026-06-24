package com.github.hkocamandev.iot_intrusion_detection.dto;

import java.util.Map;

/** /api/stats yanıtı: toplam + tip/severity/kaynak sayıları (enum adı -> sayı). */
public record StatsResponse(
        long total,
        Map<String, Long> byAttackType,
        Map<String, Long> bySeverity,
        Map<String, Long> byDetectionSource
) { }
