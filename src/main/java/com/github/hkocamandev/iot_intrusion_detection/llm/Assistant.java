package com.github.hkocamandev.iot_intrusion_detection.llm;

import dev.langchain4j.service.SystemMessage;

/** langchain4j AiService — answers NL questions about alerts using AlertQueryTools. */
public interface Assistant {

    @SystemMessage("""
            You are a security analytics assistant for an IoT intrusion-detection system.
            Use the provided tools to answer the user's questions about detected alerts.
            Prefer calling a tool over guessing. Answer concisely in plain language.""")
    String chat(String userMessage);
}
