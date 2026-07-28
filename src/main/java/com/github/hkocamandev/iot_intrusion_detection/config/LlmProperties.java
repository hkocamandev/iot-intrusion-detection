package com.github.hkocamandev.iot_intrusion_detection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

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

    public record Anthropic(String model, List<String> models) { }

    public record Gemini(String model, List<String> models) { }

    public record Groq(String model, List<String> models) { }

    /** Configured default model for a provider, or null if the provider is unknown. */
    public String defaultModelFor(String provider) {
        return switch (provider == null ? "" : provider.toLowerCase()) {
            case "anthropic" -> anthropic().model();
            case "gemini" -> gemini().model();
            case "groq" -> groq().model();
            default -> null;
        };
    }

    /** Curated selectable models for a provider, or an empty list if unknown/none. */
    public List<String> modelsFor(String provider) {
        List<String> models = switch (provider == null ? "" : provider.toLowerCase()) {
            case "anthropic" -> anthropic().models();
            case "gemini" -> gemini().models();
            case "groq" -> groq().models();
            default -> null;
        };
        return models == null ? List.of() : models;
    }
}
