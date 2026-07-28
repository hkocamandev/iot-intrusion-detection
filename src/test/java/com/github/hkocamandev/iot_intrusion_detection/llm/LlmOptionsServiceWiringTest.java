package com.github.hkocamandev.iot_intrusion_detection.llm;

import com.github.hkocamandev.iot_intrusion_detection.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the Spring wiring of {@link LlmOptionsService}.
 *
 * <p>Registers the service by TYPE via {@code @Import} (component-style registration), the same
 * way {@code @Service} + component-scan would pick it up in the real application context. This
 * forces Spring's constructor-resolution algorithm to run against the real class, which is what
 * reproduces the original "No default constructor found" failure when the class has two eligible
 * constructors and no {@code @Autowired} marker. An explicit {@code @Bean} factory method would
 * NOT reproduce this bug, because the factory method itself picks the constructor.
 */
class LlmOptionsServiceWiringTest {

    @Configuration
    @EnableConfigurationProperties(LlmProperties.class)
    @Import(LlmOptionsService.class)
    static class Config {
    }

    @Test
    void serviceIsConstructedViaSpring() {
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "app.llm.enabled=true",
                        "app.llm.provider=groq",
                        "app.llm.timeout=30s",
                        "app.llm.enrichment.batch-size=10",
                        "app.llm.enrichment.poll-interval=10s",
                        "app.llm.anthropic.model=claude-haiku-4-5",
                        "app.llm.anthropic.models[0]=claude-haiku-4-5",
                        "app.llm.gemini.model=gemini-2.0-flash",
                        "app.llm.gemini.models[0]=gemini-2.0-flash",
                        "app.llm.groq.model=llama-3.3-70b-versatile",
                        "app.llm.groq.models[0]=llama-3.3-70b-versatile",
                        "app.llm.groq.models[1]=openai/gpt-oss-120b")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(LlmOptionsService.class);
                });
    }
}
