package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.Alert;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {

    private final AlertRepository repository;

    public AlertConsumer(AlertRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.topics.alerts}", groupId = "alert-processor")
    public void onAlert(AlertEvent event) {
        Alert alert = new Alert(
                event.detectedAt(), event.sourceId(), event.detectionSource(),
                event.attackType(), event.severity(), event.score(), event.ruleName(),
                event.protocol(), event.service(), event.trueLabel());
        repository.save(alert);
    }
}
