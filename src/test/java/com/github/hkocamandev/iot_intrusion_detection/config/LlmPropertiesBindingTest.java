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
                    "app.llm.model=claude-haiku-4-5",
                    "app.llm.timeout=30s",
                    "app.llm.enrichment.batch-size=10",
                    "app.llm.enrichment.poll-interval=10s");

    @EnableConfigurationProperties(LlmProperties.class)
    static class Config { }

    @Test
    void bindsLlmProperties() {
        runner.run(ctx -> {
            LlmProperties props = ctx.getBean(LlmProperties.class);
            assertThat(props.enabled()).isTrue();
            assertThat(props.model()).isEqualTo("claude-haiku-4-5");
            assertThat(props.timeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.enrichment().batchSize()).isEqualTo(10);
            assertThat(props.enrichment().pollInterval()).isEqualTo(Duration.ofSeconds(10));
        });
    }
}
