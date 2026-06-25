package com.github.hkocamandev.iot_intrusion_detection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.llm.enabled=false")
class IotIntrusionDetectionApplicationTests {

    @Test
    void contextLoads() {
    }

}
