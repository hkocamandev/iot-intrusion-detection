package com.github.hkocamandev.iot_intrusion_detection.config;

import com.github.hkocamandev.iot_intrusion_detection.llm.AlertQueryTools;
import com.github.hkocamandev.iot_intrusion_detection.llm.Assistant;
import com.github.hkocamandev.iot_intrusion_detection.llm.SecurityAnalyst;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
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
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public ChatModel chatModel(ChatModelFactory factory, LlmProperties properties) {
        return factory.create(properties.provider(), properties.defaultModelFor(properties.provider()));
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
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
}
