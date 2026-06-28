package com.github.hkocamandev.iot_intrusion_detection.config;

import com.github.hkocamandev.iot_intrusion_detection.llm.AlertQueryTools;
import com.github.hkocamandev.iot_intrusion_detection.llm.Assistant;
import com.github.hkocamandev.iot_intrusion_detection.llm.SecurityAnalyst;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Hand-wires langchain4j beans, selecting the provider via app.llm.provider.
 *  Active only when app.llm.enabled=true (no API key needed otherwise). */
@Configuration
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
@EnableScheduling
public class LlmConfig {

    @Bean
    public ChatModel chatModel(LlmProperties properties) {
        String provider = properties.provider();
        return switch (provider == null ? "" : provider.toLowerCase()) {
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                    .modelName(properties.anthropic().model())
                    .timeout(properties.timeout())
                    .build();
            case "gemini" -> GoogleAiGeminiChatModel.builder()
                    .apiKey(System.getenv("GEMINI_API_KEY"))
                    .modelName(properties.gemini().model())
                    .timeout(properties.timeout())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unknown app.llm.provider '" + provider
                            + "'. Valid values: anthropic, gemini.");
        };
    }

    @Bean
    public SecurityAnalyst securityAnalyst(ChatModel chatModel) {
        return AiServices.create(SecurityAnalyst.class, chatModel);
    }

    @Bean
    public Assistant assistant(ChatModel chatModel, AlertQueryTools alertQueryTools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(alertQueryTools)
                .build();
    }
}
