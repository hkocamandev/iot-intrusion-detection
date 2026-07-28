package com.github.hkocamandev.iot_intrusion_detection.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigTest {

    private static LlmProperties props(String provider) {
        return new LlmProperties(true, provider, Duration.ofSeconds(30),
                new LlmProperties.Enrichment(10, Duration.ofSeconds(10)),
                new LlmProperties.Anthropic("claude-haiku-4-5", java.util.List.of("claude-haiku-4-5")),
                new LlmProperties.Gemini("gemini-2.0-flash", java.util.List.of("gemini-2.0-flash")),
                new LlmProperties.Groq("llama-3.3-70b-versatile",
                        java.util.List.of("llama-3.3-70b-versatile", "openai/gpt-oss-120b")));
    }

    @Test
    void unknownProviderFailsFast() {
        assertThatThrownBy(() -> new LlmConfig().chatModel(props("bogus")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anthropic")
                .hasMessageContaining("gemini")
                .hasMessageContaining("groq");
    }
}
