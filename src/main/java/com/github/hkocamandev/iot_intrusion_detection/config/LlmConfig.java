package com.github.hkocamandev.iot_intrusion_detection.config;

import com.github.hkocamandev.iot_intrusion_detection.llm.SecurityAnalyst;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Hand-wires langchain4j beans. Active only when app.llm.enabled=true (no API key needed otherwise). */
@Configuration
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
@EnableScheduling
public class LlmConfig {

    @Bean
    public ChatModel claudeChatModel(LlmProperties properties) {
        return AnthropicChatModel.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName(properties.model())
                .timeout(properties.timeout())
                .build();
    }

    @Bean
    public SecurityAnalyst securityAnalyst(ChatModel chatModel) {
        return AiServices.create(SecurityAnalyst.class, chatModel);
    }
}
