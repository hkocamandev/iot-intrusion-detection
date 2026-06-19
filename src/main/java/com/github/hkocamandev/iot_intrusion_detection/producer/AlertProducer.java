package com.github.hkocamandev.iot_intrusion_detection.producer;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public AlertProducer(KafkaTemplate<String, Object> kafkaTemplate,
                         @Value("${app.topics.alerts}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(AlertEvent alert) {
        kafkaTemplate.send(topic, alert.sourceId(), alert)
                .exceptionally(ex -> {
                    log.error("Alert send failed for {}", alert.sourceId(), ex);
                    return null;
                });
    }
}
