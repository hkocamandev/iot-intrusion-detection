package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** ML tahminini AlertEvent'e çevirir: NORMAL veya düşük güven -> alarm yok. */
@Component
public class MlAlertMapper {

    private static final String ML_SOURCE = "ml-xgboost";

    private final MlProperties properties;

    public MlAlertMapper(MlProperties properties) {
        this.properties = properties;
    }

    public Optional<AlertEvent> toAlert(TrafficEvent event, MlPrediction prediction) {
        AttackType type;
        try {
            type = AttackType.valueOf(prediction.attackType());
        } catch (IllegalArgumentException | NullPointerException e) {
            type = AttackType.OTHER;
        }
        if (type == AttackType.NORMAL) return Optional.empty();
        if (prediction.score() < properties.minScore()) return Optional.empty();

        Map<AttackType, Severity> map = properties.severityByType();
        Severity severity = (map == null) ? Severity.MEDIUM : map.getOrDefault(type, Severity.MEDIUM);

        return Optional.of(new AlertEvent(
                Instant.now(), event.sourceId(), DetectionSource.ML,
                type, severity, prediction.score(), ML_SOURCE,
                event.protocol(), event.service(), event.trueLabel()));
    }
}
