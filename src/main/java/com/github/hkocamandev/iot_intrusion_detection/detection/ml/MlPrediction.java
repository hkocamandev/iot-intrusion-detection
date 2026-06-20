package com.github.hkocamandev.iot_intrusion_detection.detection.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/** ml-service /predict yanıtı. */
public record MlPrediction(
        @JsonProperty("attack_type") String attackType,
        @JsonProperty("score") double score
) { }
