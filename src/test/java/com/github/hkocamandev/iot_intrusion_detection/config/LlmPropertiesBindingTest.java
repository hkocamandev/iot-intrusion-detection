package com.github.hkocamandev.iot_intrusion_detection.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues(
                    "app.llm.enabled=true",
                    "app.llm.provider=gemini",
                    "app.llm.timeout=30s",
                    "app.llm.enrichment.batch-size=10",
                    "app.llm.enrichment.poll-interval=10s",
                    "app.llm.anthropic.model=claude-haiku-4-5",
                    "app.llm.gemini.model=gemini-2.0-flash");

    @EnableConfigurationProperties(LlmProperties.class)
    static class Config { }

    @Test
    void bindsLlmProperties() {
        runner.run(ctx -> {
            LlmProperties props = ctx.getBean(LlmProperties.class);
            assertThat(props.enabled()).isTrue();
            assertThat(props.provider()).isEqualTo("gemini");
            assertThat(props.timeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.enrichment().batchSize()).isEqualTo(10);
            assertThat(props.enrichment().pollInterval()).isEqualTo(Duration.ofSeconds(10));
            assertThat(props.anthropic().model()).isEqualTo("claude-haiku-4-5");
            assertThat(props.gemini().model()).isEqualTo("gemini-2.0-flash");
        });
    }
}
