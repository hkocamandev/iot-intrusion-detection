package com.github.hkocamandev.iot_intrusion_detection.dto;

/** NL chat response. provider/model report which model actually answered. */
public record ChatResponse(String answer, String sessionId, String provider, String model) { }
