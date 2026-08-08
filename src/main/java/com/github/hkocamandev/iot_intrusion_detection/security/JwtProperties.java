package com.github.hkocamandev.iot_intrusion_detection.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** app.security.jwt config binding. The secret comes from the JWT_SECRET environment variable. */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) { }
