package com.github.hkocamandev.iot_intrusion_detection.model;

/** Application roles. ANALYST reads alerts and stats; ADMIN additionally reaches the LLM endpoints. */
public enum Role {
    ADMIN,
    ANALYST;

    /** Spring Security authority name for this role. */
    public String authority() {
        return "ROLE_" + name();
    }
}
