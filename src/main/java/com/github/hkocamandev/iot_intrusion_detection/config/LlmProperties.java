package com.github.hkocamandev.iot_intrusion_detection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** app.llm config binding. API keys are NOT here — read from env per provider. */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        boolean enabled,
        String provider,
        Duration timeout,
        Enrichment enrichment,
        Anthropic anthropic,
        Gemini gemini,
        Groq groq
) {
    public record Enrichment(int batchSize, Duration pollInterval) { }

    public record Anthropic(String model) { }

    public record Gemini(String model) { }

    public record Groq(String model) { }
}
