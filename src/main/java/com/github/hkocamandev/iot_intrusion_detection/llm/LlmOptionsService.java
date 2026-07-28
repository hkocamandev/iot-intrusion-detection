package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.LlmProperties;
import com.github.hkocamandev.iot_intrusion_detection.dto.LlmOptions;
import com.github.hkocamandev.iot_intrusion_detection.dto.ProviderOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Reports the chat providers/models available (API key present) and resolves a request to a valid pair. */
@Service
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
public class LlmOptionsService {

    /** provider id -> its API-key env var name. */
    private static final List<String[]> PROVIDERS = List.of(
            new String[]{"groq", "GROQ_API_KEY"},
            new String[]{"gemini", "GEMINI_API_KEY"},
            new String[]{"anthropic", "ANTHROPIC_API_KEY"});

    private final LlmProperties properties;
    private final Predicate<String> keyPresent;

    public LlmOptionsService(LlmProperties properties) {
        this(properties, envVar -> {
            String v = System.getenv(envVar);
            return v != null && !v.isBlank();
        });
    }

    LlmOptionsService(LlmProperties properties, Predicate<String> keyPresent) {
        this.properties = properties;
        this.keyPresent = keyPresent;
    }

    public LlmOptions options() {
        List<ProviderOption> available = new ArrayList<>();
        for (String[] p : PROVIDERS) {
            String id = p[0];
            List<String> models = properties.modelsFor(id);
            if (keyPresent.test(p[1]) && !models.isEmpty()) {
                available.add(new ProviderOption(id, models));
            }
        }
        String defaultProvider = null;
        String defaultModel = null;
        if (!available.isEmpty()) {
            ProviderOption configured = available.stream()
                    .filter(o -> o.id().equals(properties.provider()))
                    .findFirst()
                    .orElse(available.get(0));
            defaultProvider = configured.id();
            defaultModel = properties.defaultModelFor(configured.id());
            if (defaultModel == null || !configured.models().contains(defaultModel)) {
                defaultModel = configured.models().get(0);
            }
        }
        return new LlmOptions(available, defaultProvider, defaultModel);
    }

    public ResolvedModel resolve(String provider, String model) {
        LlmOptions options = options();
        String normalized = provider == null ? "" : provider.toLowerCase();
        for (ProviderOption o : options.providers()) {
            if (o.id().equals(normalized) && o.models().contains(model)) {
                return new ResolvedModel(o.id(), model);
            }
        }
        return new ResolvedModel(options.defaultProvider(), options.defaultModel());
    }
}
