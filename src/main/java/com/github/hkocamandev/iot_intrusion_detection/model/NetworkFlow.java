package com.github.hkocamandev.iot_intrusion_detection.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "network_flows")
public class NetworkFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant ingestedAt;

    private String sourceId;
    private String protocol;
    private String service;

    /** RT-IoT2022 ham etiketi (ör. "DOS_SYN_Hping"). */
    private String trueLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttackType trueAttackType;

    /** Tüm sayısal özellikler; ML aşamasında kullanılacak. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> features;

    protected NetworkFlow() { }

    public NetworkFlow(Instant ingestedAt, String sourceId, String protocol, String service,
                       String trueLabel, AttackType trueAttackType, Map<String, Double> features) {
        this.ingestedAt = ingestedAt;
        this.sourceId = sourceId;
        this.protocol = protocol;
        this.service = service;
        this.trueLabel = trueLabel;
        this.trueAttackType = trueAttackType;
        this.features = features;
    }

    public Long getId() { return id; }
    public Instant getIngestedAt() { return ingestedAt; }
    public String getSourceId() { return sourceId; }
    public String getProtocol() { return protocol; }
    public String getService() { return service; }
    public String getTrueLabel() { return trueLabel; }
    public AttackType getTrueAttackType() { return trueAttackType; }
    public Map<String, Double> getFeatures() { return features; }
}
