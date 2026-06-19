package com.github.hkocamandev.iot_intrusion_detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IotIntrusionDetectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotIntrusionDetectionApplication.class, args);
	}

}
