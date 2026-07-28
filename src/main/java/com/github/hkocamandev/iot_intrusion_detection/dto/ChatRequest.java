package com.github.hkocamandev.iot_intrusion_detection.dto;

/** NL chat request. sessionId/provider/model are optional; blank means "use the default". */
public record ChatRequest(String question, String sessionId, String provider, String model) { }
