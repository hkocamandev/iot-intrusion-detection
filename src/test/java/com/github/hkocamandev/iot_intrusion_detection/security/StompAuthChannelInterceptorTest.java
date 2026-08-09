package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StompAuthChannelInterceptorTest {

    private static final String SECRET = "test-only-signing-secret-at-least-32-bytes-long";
    private static final String ISSUER = "iot-intrusion-detection";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1), ISSUER);
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(jwtService);

    private Message<byte[]> frame(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void bindsThePrincipalOnValidConnect() {
        String token = jwtService.issue("alice", Role.ADMIN).token();

        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + token), null);

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("alice");
    }

    @Test
    void rejectsConnectWithoutAuthorizationHeader() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void rejectsConnectWithNonBearerHeader() {
        assertThatThrownBy(() ->
                interceptor.preSend(frame(StompCommand.CONNECT, "Basic dXNlcjpwYXNz"), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void rejectsConnectWithExpiredToken() {
        JwtService expiring = new JwtService(SECRET, Duration.ofMinutes(-5), ISSUER);
        String expired = expiring.issue("alice", Role.ADMIN).token();

        assertThatThrownBy(() ->
                interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + expired), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void rejectsConnectWithForgedSignature() {
        JwtService forger = new JwtService("a-totally-different-secret-of-sufficient-length",
                Duration.ofHours(1), ISSUER);
        String forged = forger.issue("alice", Role.ADMIN).token();

        assertThatThrownBy(() ->
                interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + forged), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void passesNonConnectCommandsThrough() {
        // The session is already bound at CONNECT, so later frames need no header.
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null);

        assertThat(interceptor.preSend(subscribe, null)).isSameAs(subscribe);
    }
}
