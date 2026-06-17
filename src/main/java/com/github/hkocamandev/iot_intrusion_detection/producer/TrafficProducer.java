package com.github.hkocamandev.iot_intrusion_detection.producer;

import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrafficProducer {

    private static final Logger log = LoggerFactory.getLogger(TrafficProducer.class);

    private final KafkaTemplate<String, TrafficEvent> kafkaTemplate;
    private final String topic;

    public TrafficProducer(KafkaTemplate<String, TrafficEvent> kafkaTemplate,
                           @Value("${app.topics.traffic-raw}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TrafficEvent event) {
        kafkaTemplate.send(topic, event.sourceId(), event)
                .exceptionally(ex -> {
                    log.error("Kafka send failed for {}", event.sourceId(), ex);
                    return null;
                });
    }
}
