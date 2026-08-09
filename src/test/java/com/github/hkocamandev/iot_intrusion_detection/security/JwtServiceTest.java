package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-of-adequate-length";
    private static final String ISSUER = "iot-intrusion-detection";

    private JwtService service() {
        return new JwtService(SECRET, Duration.ofHours(1), ISSUER);
    }

    @Test
    void roundTripsUsernameAndRole() {
        IssuedToken issued = service().issue("alice", Role.ADMIN);

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());

        JwtPrincipal principal = service().parse(issued.token()).orElseThrow();
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void expiresAtReflectsConfiguredTtl() {
        JwtService shortLived = new JwtService(SECRET, Duration.ofMinutes(15), ISSUER);

        IssuedToken issued = shortLived.issue("alice", Role.ANALYST);

        assertThat(issued.expiresAt()).isBetween(
                Instant.now().plus(Duration.ofMinutes(14)),
                Instant.now().plus(Duration.ofMinutes(16)));
    }

    @Test
    void rejectsExpiredToken() {
        // A negative TTL mints a token whose exp is already in the past.
        JwtService expiring = new JwtService(SECRET, Duration.ofMinutes(-5), ISSUER);
        String token = expiring.issue("alice", Role.ADMIN).token();

        assertThat(service().parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService attacker = new JwtService(OTHER_SECRET, Duration.ofHours(1), ISSUER);
        String forged = attacker.issue("alice", Role.ADMIN).token();

        assertThat(service().parse(forged)).isEmpty();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtService foreign = new JwtService(SECRET, Duration.ofHours(1), "some-other-app");
        String token = foreign.issue("alice", Role.ADMIN).token();

        assertThat(service().parse(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(service().parse("not-a-jwt")).isEmpty();
        assertThat(service().parse("")).isEmpty();
        assertThat(service().parse(null)).isEmpty();
    }

    @Test
    void rejectsUnknownRoleClaim() {
        // Correctly signed with the right key and issuer, but the role claim is not a Role
        // constant — so this isolates the claim check from the signature check.
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .claim("role", "SUPERUSER")
                .issuer(ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThat(service().parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithNoRoleClaim() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .issuer(ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThat(service().parse(token)).isEmpty();
    }

    @Test
    void refusesConstructionWithTooShortSecret() {
        assertThatThrownBy(() -> new JwtService("short", Duration.ofHours(1), ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret");
    }

    @Test
    void refusesConstructionWithNullSecret() {
        assertThatThrownBy(() -> new JwtService(null, Duration.ofHours(1), ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret");
    }

    @Test
    void authorityStringsMatchSpringConvention() {
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ANALYST.authority()).isEqualTo("ROLE_ANALYST");
    }
}
