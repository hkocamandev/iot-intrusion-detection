package com.github.hkocamandev.iot_intrusion_detection.dto;

/** NL chat response body. sessionId is the (possibly server-generated) conversation id. */
public record ChatResponse(String answer, String sessionId) { }
