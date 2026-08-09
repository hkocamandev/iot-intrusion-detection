package com.github.hkocamandev.iot_intrusion_detection.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.security.seed config binding. The password comes from SEED_ADMIN_PASSWORD. */
@ConfigurationProperties(prefix = "app.security.seed")
public record SeedProperties(String adminUsername, String adminPassword) { }
