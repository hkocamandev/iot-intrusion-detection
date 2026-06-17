package com.github.hkocamandev.iot_intrusion_detection.dto;

import java.time.Instant;
import java.util.Map;

/** Kafka iot.traffic.raw mesajı. JSON serileştirme spring-kafka JsonSerializer ile. */
public record TrafficEvent(
        Instant ingestedAt,
        String sourceId,
        String protocol,
        String service,
        String trueLabel,
        Map<String, Double> features
) { }
