package com.github.hkocamandev.iot_intrusion_detection.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DetectionSource detectionSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttackType attackType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    private double score;
    private String ruleName;
    private String protocol;
    private String service;
    private String trueLabel;

    // Plan 5 (LLM) tarafından doldurulacak — şimdilik null
    @Column(length = 2000)
    private String llmExplanation;
    @Column(length = 2000)
    private String llmRecommendation;
    private Instant llmEnrichedAt;

    protected Alert() { }

    public Alert(Instant createdAt, String sourceId, DetectionSource detectionSource,
                 AttackType attackType, Severity severity, double score, String ruleName,
                 String protocol, String service, String trueLabel) {
        this.createdAt = createdAt;
        this.sourceId = sourceId;
        this.detectionSource = detectionSource;
        this.attackType = attackType;
        this.severity = severity;
        this.score = score;
        this.ruleName = ruleName;
        this.protocol = protocol;
        this.service = service;
        this.trueLabel = trueLabel;
    }

    public void applyLlmEnrichment(String explanation, String recommendation, Instant enrichedAt) {
        this.llmExplanation = explanation;
        this.llmRecommendation = recommendation;
        this.llmEnrichedAt = enrichedAt;
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSourceId() { return sourceId; }
    public DetectionSource getDetectionSource() { return detectionSource; }
    public AttackType getAttackType() { return attackType; }
    public Severity getSeverity() { return severity; }
    public double getScore() { return score; }
    public String getRuleName() { return ruleName; }
    public String getProtocol() { return protocol; }
    public String getService() { return service; }
    public String getTrueLabel() { return trueLabel; }
    public String getLlmExplanation() { return llmExplanation; }
    public String getLlmRecommendation() { return llmRecommendation; }
    public Instant getLlmEnrichedAt() { return llmEnrichedAt; }
}
