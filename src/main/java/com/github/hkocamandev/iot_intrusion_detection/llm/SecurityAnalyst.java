package com.github.hkocamandev.iot_intrusion_detection.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/** langchain4j AiService — turns an alert summary into an explanation + recommendation. */
public interface SecurityAnalyst {

    @SystemMessage("""
            You are a senior SOC (Security Operations Center) analyst for an IoT network
            intrusion-detection system. Given a single alert, produce:
            - explanation: a concise, plain-language description of what this alert likely means.
            - recommendation: a concrete, actionable next step for a responder.
            Keep each field under 1500 characters.""")
    Enrichment analyze(@UserMessage String alertSummary);
}
