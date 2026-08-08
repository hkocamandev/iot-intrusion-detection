package com.github.hkocamandev.iot_intrusion_detection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registration input. There is deliberately no role field: self-registration is always ANALYST. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 200) String password
) { }
