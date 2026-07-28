package com.github.hkocamandev.iot_intrusion_detection.llm;

/** An answer plus the provider/model that actually produced it. */
public record ChatAnswer(String answer, String provider, String model) { }
