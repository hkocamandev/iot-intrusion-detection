package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertView;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {"app.ml.enabled=false", "app.llm.enabled=false"})
class AlertWebSocketIT {

    @LocalServerPort int port;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${app.topics.alerts}") String alertsTopic;
    @Autowired JwtService jwtService;

    @Test
    void alertIsPushedOverWebSocket() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
        stompClient.setMessageConverter(converter);

        BlockingQueue<AlertView> received = new LinkedBlockingQueue<>();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization",
                "Bearer " + jwtService.issue("ws-tester", Role.ANALYST).token());
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                        connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        session.subscribe("/topic/alerts", new StompFrameHandler() {
            @Override public @NonNull Type getPayloadType(@NonNull StompHeaders headers) { return AlertView.class; }
            @Override public void handleFrame(@NonNull StompHeaders headers, Object payload) { received.add((AlertView) payload); }
        });
        Thread.sleep(500); // let the subscription register

        AlertEvent event = new AlertEvent(Instant.now(), "ws-1", DetectionSource.RULE,
                AttackType.DOS, Severity.HIGH, 5.0, "dos-high-packet-rate", "tcp", "http", "x");
        kafkaTemplate.send(alertsTopic, "ws-1", event);

        AlertView view = received.poll(20, TimeUnit.SECONDS);
        assertThat(view).isNotNull();
        assertThat(view.sourceId()).isEqualTo("ws-1");
        assertThat(view.attackType()).isEqualTo(AttackType.DOS);
        assertThat(view.detectionSource()).isEqualTo(DetectionSource.RULE);
    }

    @Test
    void connectWithoutTokenIsRejected() {
        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));

        // No token in connectHeaders: the CONNECT frame must be refused, so the future never
        // completes normally. assertThatThrownBy fails the test if nothing is raised.
        assertThatThrownBy(() -> stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }
}
