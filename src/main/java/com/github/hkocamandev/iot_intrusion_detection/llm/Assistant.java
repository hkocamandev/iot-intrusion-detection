package com.github.hkocamandev.iot_intrusion_detection.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/** langchain4j AiService — answers NL questions about alerts using AlertQueryTools.
 *  Conversations are grouped per session via the {@code sessionId} memory id. */
public interface Assistant {

    @SystemMessage("""
            You are a security analytics assistant for an IoT intrusion-detection system.
            Use the provided tools to answer the user's questions about detected alerts.
            Prefer calling a tool over guessing. Answer concisely in plain language.""")
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
