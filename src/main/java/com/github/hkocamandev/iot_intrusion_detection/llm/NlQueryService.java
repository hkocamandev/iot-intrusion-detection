package com.github.hkocamandev.iot_intrusion_detection.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Answers natural-language questions about alerts. Active when app.llm.enabled=true. */
@Service
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class NlQueryService {

    private final AssistantRegistry registry;
    private final LlmOptionsService options;

    public NlQueryService(AssistantRegistry registry, LlmOptionsService options) {
        this.registry = registry;
        this.options = options;
    }

    /** Resolves the requested provider/model (falling back to the default) and answers. */
    public ChatAnswer ask(String sessionId, String question, String provider, String model) {
        ResolvedModel resolved = options.resolve(provider, model);
        String answer = registry.get(resolved.provider(), resolved.model()).chat(sessionId, question);
        return new ChatAnswer(answer, resolved.provider(), resolved.model());
    }
}
