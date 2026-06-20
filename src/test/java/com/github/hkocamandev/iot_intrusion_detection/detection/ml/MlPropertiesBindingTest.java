package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MlPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues(
                    "app.ml.enabled=true",
                    "app.ml.url=http://localhost:8000",
                    "app.ml.timeout-ms=2000",
                    "app.ml.min-score=0.5",
                    "app.ml.severity-by-type.DOS=HIGH",
                    "app.ml.severity-by-type.RECON=MEDIUM");

    @EnableConfigurationProperties(MlProperties.class)
    static class Config { }

    @Test
    void bindsMlProperties() {
        runner.run(ctx -> {
            MlProperties props = ctx.getBean(MlProperties.class);
            assertThat(props.enabled()).isTrue();
            assertThat(props.timeoutMs()).isEqualTo(2000);
            assertThat(props.minScore()).isEqualTo(0.5);
            assertThat(props.severityByType())
                    .containsEntry(AttackType.DOS, Severity.HIGH)
                    .containsEntry(AttackType.RECON, Severity.MEDIUM);
        });
    }
}
