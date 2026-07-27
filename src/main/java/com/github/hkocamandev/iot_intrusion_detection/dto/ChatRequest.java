package com.github.hkocamandev.iot_intrusion_detection.dto;

/** NL chat request body. sessionId is optional; blank/absent means "start a new session". */
public record ChatRequest(String question, String sessionId) { }
