package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.LlmProperties;
import com.github.hkocamandev.iot_intrusion_detection.dto.LlmOptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class LlmOptionsServiceTest {

    private static LlmProperties props() {
        return new LlmProperties(true, "groq", Duration.ofSeconds(30),
                new LlmProperties.Enrichment(10, Duration.ofSeconds(10)),
                new LlmProperties.Anthropic("claude-haiku-4-5", List.of("claude-haiku-4-5")),
                new LlmProperties.Gemini("gemini-2.0-flash", List.of("gemini-2.0-flash")),
                new LlmProperties.Groq("llama-3.3-70b-versatile",
                        List.of("llama-3.3-70b-versatile", "openai/gpt-oss-120b")));
    }

    // Only GROQ_API_KEY is "present".
    private final Predicate<String> onlyGroq = Set.of("GROQ_API_KEY")::contains;
    private final LlmOptionsService service = new LlmOptionsService(props(), onlyGroq);

    @Test
    void onlyProvidersWithAKeyAreOffered() {
        LlmOptions options = service.options();
        assertThat(options.providers()).hasSize(1);
        assertThat(options.providers().get(0).id()).isEqualTo("groq");
        assertThat(options.providers().get(0).models())
                .containsExactly("llama-3.3-70b-versatile", "openai/gpt-oss-120b");
        assertThat(options.defaultProvider()).isEqualTo("groq");
        assertThat(options.defaultModel()).isEqualTo("llama-3.3-70b-versatile");
    }

    @Test
    void resolveKeepsAValidPair() {
        ResolvedModel r = service.resolve("groq", "openai/gpt-oss-120b");
        assertThat(r.provider()).isEqualTo("groq");
        assertThat(r.model()).isEqualTo("openai/gpt-oss-120b");
    }

    @Test
    void resolveIsCaseInsensitiveOnProvider() {
        ResolvedModel r = service.resolve("GROQ", "openai/gpt-oss-120b");
        assertThat(r.provider()).isEqualTo("groq");
        assertThat(r.model()).isEqualTo("openai/gpt-oss-120b");
    }

    @Test
    void resolveFallsBackToDefaultForUnavailableProviderOrModel() {
        assertThat(service.resolve("gemini", "gemini-2.0-flash"))
                .isEqualTo(new ResolvedModel("groq", "llama-3.3-70b-versatile"));
        assertThat(service.resolve("groq", "not-a-real-model"))
                .isEqualTo(new ResolvedModel("groq", "llama-3.3-70b-versatile"));
        assertThat(service.resolve(null, null))
                .isEqualTo(new ResolvedModel("groq", "llama-3.3-70b-versatile"));
    }
}
