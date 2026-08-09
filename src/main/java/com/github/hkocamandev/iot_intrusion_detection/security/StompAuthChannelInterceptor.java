package com.github.hkocamandev.iot_intrusion_detection.security;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame. A browser cannot set an Authorization header on a
 * WebSocket handshake, so the token travels in the CONNECT frame instead — which also keeps it
 * out of URLs, access logs and proxy logs. Later frames need no header: the session is bound here.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }
        JwtPrincipal principal = principalFrom(accessor);
        if (principal == null) {
            // Surfaces to the client as a STOMP ERROR frame; no subscription is ever opened.
            throw new MessageDeliveryException(message, "Unauthorized STOMP CONNECT");
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                principal.username(), null,
                List.of(new SimpleGrantedAuthority(principal.role().authority()))));
        return message;
    }

    private JwtPrincipal principalFrom(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return jwtService.parse(header.substring(BEARER_PREFIX.length())).orElse(null);
    }
}
