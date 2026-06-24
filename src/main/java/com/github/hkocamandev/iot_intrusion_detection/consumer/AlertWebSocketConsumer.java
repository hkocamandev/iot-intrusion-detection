package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** iot.alerts (grup dashboard) -> STOMP /topic/alerts. Push hatası yutulur (kritik değil). */
@Component
public class AlertWebSocketConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertWebSocketConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AlertWebSocketConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "${app.topics.alerts}", groupId = "dashboard")
    public void onAlert(AlertEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/alerts", AlertView.from(event));
        } catch (Exception e) {
            log.warn("WebSocket push failed for {}: {}", event.sourceId(), e.toString());
        }
    }
}
