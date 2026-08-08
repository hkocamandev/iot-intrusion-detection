package com.github.hkocamandev.iot_intrusion_detection.security;

import java.time.Instant;

/** A freshly minted token plus its expiry, so callers need not re-parse it. */
public record IssuedToken(String token, Instant expiresAt) { }
