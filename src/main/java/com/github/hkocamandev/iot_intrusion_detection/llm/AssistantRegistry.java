package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.ChatModelFactory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Caches one Assistant per (provider, model); all share one memory store so history is per-session. */
@Component
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class AssistantRegistry {

    private final ChatModelFactory factory;
    private final AlertQueryTools tools;
    private final ChatMemoryStore memoryStore;
    private final Map<String, Assistant> cache = new ConcurrentHashMap<>();

    public AssistantRegistry(ChatModelFactory factory, AlertQueryTools tools, ChatMemoryStore memoryStore) {
        this.factory = factory;
        this.tools = tools;
        this.memoryStore = memoryStore;
    }

    public Assistant get(String provider, String model) {
        return cache.computeIfAbsent(provider + "|" + model, key -> AiServices.builder(Assistant.class)
                .chatModel(factory.create(provider, model))
                .tools(tools)
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .chatMemoryStore(memoryStore)
                        .maxMessages(10)
                        .build())
                .build());
    }
}
