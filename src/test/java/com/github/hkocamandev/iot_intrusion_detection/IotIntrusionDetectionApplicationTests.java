package com.github.hkocamandev.iot_intrusion_detection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IotIntrusionDetectionApplicationTests {

    @Test
    void contextLoads() {
    }

}
