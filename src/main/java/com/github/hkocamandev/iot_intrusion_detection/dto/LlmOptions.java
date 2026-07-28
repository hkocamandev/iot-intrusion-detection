package com.github.hkocamandev.iot_intrusion_detection.dto;

import java.util.List;

/** Chat LLM options for the UI: available providers plus the default selection. */
public record LlmOptions(List<ProviderOption> providers, String defaultProvider, String defaultModel) { }
