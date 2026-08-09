package com.github.hkocamandev.iot_intrusion_detection.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Flat error body. `fields` is present only for validation failures. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, Map<String, String> fields) {

    public ApiError(String error) {
        this(error, null);
    }
}
