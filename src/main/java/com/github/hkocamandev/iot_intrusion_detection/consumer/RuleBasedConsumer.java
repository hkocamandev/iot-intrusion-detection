package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.detection.rulebased.RuleEngine;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.producer.AlertProducer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleBasedConsumer {

    private final RuleEngine ruleEngine;
    private final AlertProducer alertProducer;

    public RuleBasedConsumer(RuleEngine ruleEngine, AlertProducer alertProducer) {
        this.ruleEngine = ruleEngine;
        this.alertProducer = alertProducer;
    }

    @KafkaListener(topics = "${app.topics.traffic-raw}", groupId = "rule-engine")
    public void onTraffic(TrafficEvent event) {
        List<AlertEvent> alerts = ruleEngine.evaluate(event);
        for (AlertEvent alert : alerts) {
            alertProducer.publish(alert);
        }
    }
}
