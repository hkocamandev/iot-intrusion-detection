package com.github.hkocamandev.iot_intrusion_detection.llm;

/** LLM enrichment payload for an alert. */
public record Enrichment(String explanation, String recommendation) { }
