package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.NetworkFlow;
import com.github.hkocamandev.iot_intrusion_detection.repository.NetworkFlowRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StorageConsumer {

    private final NetworkFlowRepository repository;

    public StorageConsumer(NetworkFlowRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.topics.traffic-raw}", groupId = "storage")
    public void onTraffic(TrafficEvent event) {
        NetworkFlow flow = new NetworkFlow(
                event.ingestedAt(),
                event.sourceId(),
                event.protocol(),
                event.service(),
                event.trueLabel(),
                AttackType.fromLabel(event.trueLabel()),
                event.features());
        repository.save(flow);
    }
}
