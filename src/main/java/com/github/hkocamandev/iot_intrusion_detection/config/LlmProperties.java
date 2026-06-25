package com.github.hkocamandev.iot_intrusion_detection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** app.llm config binding. API key is NOT here — read from env ANTHROPIC_API_KEY. */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        boolean enabled,
        String model,
        Duration timeout,
        Enrichment enrichment
) {
    public record Enrichment(int batchSize, Duration pollInterval) { }
}
