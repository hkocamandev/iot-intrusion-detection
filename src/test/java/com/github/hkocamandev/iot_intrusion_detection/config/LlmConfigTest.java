package com.github.hkocamandev.iot_intrusion_detection.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigTest {

    private static LlmProperties props(String provider) {
        return new LlmProperties(true, provider, Duration.ofSeconds(30),
                new LlmProperties.Enrichment(10, Duration.ofSeconds(10)),
                new LlmProperties.Anthropic("claude-haiku-4-5"),
                new LlmProperties.Gemini("gemini-2.0-flash"));
    }

    @Test
    void unknownProviderFailsFast() {
        assertThatThrownBy(() -> new LlmConfig().chatModel(props("bogus")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anthropic")
                .hasMessageContaining("gemini");
    }
}
