package com.github.hkocamandev.iot_intrusion_detection.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatModelFactoryTest {

    private static LlmProperties props() {
        return new LlmProperties(true, "groq", Duration.ofSeconds(30),
                new LlmProperties.Enrichment(10, Duration.ofSeconds(10)),
                new LlmProperties.Anthropic("claude-haiku-4-5", List.of("claude-haiku-4-5")),
                new LlmProperties.Gemini("gemini-2.0-flash", List.of("gemini-2.0-flash")),
                new LlmProperties.Groq("llama-3.3-70b-versatile",
                        List.of("llama-3.3-70b-versatile", "openai/gpt-oss-120b")));
    }

    @Test
    void unknownProviderFailsFast() {
        assertThatThrownBy(() -> new ChatModelFactory(props()).create("bogus", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anthropic")
                .hasMessageContaining("gemini")
                .hasMessageContaining("groq");
    }
}
