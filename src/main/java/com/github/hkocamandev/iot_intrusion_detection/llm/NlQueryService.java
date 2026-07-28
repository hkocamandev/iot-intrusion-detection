package com.github.hkocamandev.iot_intrusion_detection.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Answers natural-language questions about alerts. Active when app.llm.enabled=true. */
@Service
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class NlQueryService {

    private final Assistant assistant;

    public NlQueryService(Assistant assistant) {
        this.assistant = assistant;
    }

    public String ask(String sessionId, String question) {
        return assistant.chat(sessionId, question);
    }
}
