package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.detection.ml.MlAlertMapper;
import com.github.hkocamandev.iot_intrusion_detection.detection.ml.MlClient;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.producer.AlertProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** iot.traffic.raw (grup ml-engine) -> ML servisi -> ML alarmı. app.ml.enabled=true ise aktif. */
@Component
@ConditionalOnProperty(name = "app.ml.enabled", havingValue = "true")
public class MlBasedConsumer {

    private final MlClient mlClient;
    private final MlAlertMapper mapper;
    private final AlertProducer alertProducer;

    public MlBasedConsumer(MlClient mlClient, MlAlertMapper mapper, AlertProducer alertProducer) {
        this.mlClient = mlClient;
        this.mapper = mapper;
        this.alertProducer = alertProducer;
    }

    @KafkaListener(topics = "${app.topics.traffic-raw}", groupId = "ml-engine")
    public void onTraffic(TrafficEvent event) {
        mlClient.predict(event.features())
                .flatMap(prediction -> mapper.toAlert(event, prediction))
                .ifPresent(alertProducer::publish);
    }
}
