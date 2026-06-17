package com.github.hkocamandev.iot_intrusion_detection.model;

/** RT-IoT2022 etiketleri ham string olarak da saklanır; bu enum bilinen
 *  kategorileri tiplemek için. NORMAL = saldırı yok. */
public enum AttackType {
    NORMAL, DOS, DDOS, PORT_SCAN, BRUTE_FORCE, RECON, OTHER;

    public static AttackType fromLabel(String label) {
        if (label == null || label.isBlank()) return OTHER;
        String l = label.toLowerCase();
        if (l.contains("normal") || l.contains("benign")) return NORMAL;
        if (l.contains("ddos")) return DDOS;
        if (l.contains("dos")) return DOS;
        if (l.contains("scan")) return PORT_SCAN;
        if (l.contains("brute")) return BRUTE_FORCE;
        if (l.contains("recon") || l.contains("nmap")) return RECON;
        return OTHER;
    }
}
