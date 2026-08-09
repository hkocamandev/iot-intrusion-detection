package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.github.hkocamandev.iot_intrusion_detection.model.Role;

import java.time.Instant;

/** Login result. expiresAt is informational for the client; the server re-checks exp on every call. */
public record AuthResponse(String token, String username, Role role, Instant expiresAt) { }
