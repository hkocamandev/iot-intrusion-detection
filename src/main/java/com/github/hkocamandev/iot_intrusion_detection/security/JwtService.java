package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Mints and validates HS256 access tokens. Deliberately free of Spring dependencies so the
 * signature, expiry and issuer rules can be tested without an application context.
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration ttl;
    private final String issuer;

    public JwtService(String secret, Duration ttl, String issuer) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be set to at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256. Set the JWT_SECRET environment variable, "
                            + "e.g. JWT_SECRET=$(openssl rand -base64 48)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.issuer = issuer;
    }

    /** Mints a token for the given identity. */
    public IssuedToken issue(String username, Role role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String token = Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role.name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * Validates signature, expiry and issuer, and reads the identity from the claims.
     * Returns empty on any failure — callers get no exception, because an invalid token is a
     * normal, client-triggerable event rather than an error condition.
     */
    public Optional<JwtPrincipal> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
            return Optional.of(new JwtPrincipal(claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            // DEBUG, not WARN: any client can trigger this at will, so higher levels are a log-flood vector.
            log.debug("Rejected token: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
