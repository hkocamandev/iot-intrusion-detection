package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.Role;

/** The identity carried by a validated token. Read from claims only — never from the database. */
public record JwtPrincipal(String username, Role role) { }
