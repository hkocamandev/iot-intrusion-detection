package com.github.hkocamandev.iot_intrusion_detection.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Builds a langchain4j ChatModel for a given provider + model. API keys come from env. */
@Component
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class ChatModelFactory {

    private final Duration timeout;

    public ChatModelFactory(LlmProperties properties) {
        this.timeout = properties.timeout();
    }

    public ChatModel create(String provider, String model) {
        return switch (provider == null ? "" : provider.toLowerCase()) {
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                    .modelName(model)
                    .timeout(timeout)
                    .build();
            case "gemini" -> GoogleAiGeminiChatModel.builder()
                    .apiKey(System.getenv("GEMINI_API_KEY"))
                    .modelName(model)
                    .timeout(timeout)
                    .build();
            case "groq" -> OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(System.getenv("GROQ_API_KEY"))
                    .modelName(model)
                    .timeout(timeout)
                    // Fail fast on rate-limits: a token limit won't clear within the retry
                    // window, so retrying only adds latency before the same failure.
                    .maxRetries(1)
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unknown app.llm.provider '" + provider
                            + "'. Valid values: anthropic, gemini, groq.");
        };
    }
}
