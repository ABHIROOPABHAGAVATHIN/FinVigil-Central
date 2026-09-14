package com.finvigil.aml.entity;

import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "aml_alerts")
public class AmlAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_uuid", nullable = false, unique = true, length = 64)
    private String alertUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "transaction_uuid", length = 64)
    private String transactionUuid;

    @Column(name = "rule_score", nullable = false)
    private Double ruleScore = 0.0;

    @Column(name = "anomaly_score", nullable = false)
    private Double anomalyScore = 0.0;

    @Column(name = "ml_score")
    private Double mlScore = 0.0;

    @Column(name = "hybrid_score", nullable = false)
    private Double hybridScore = 0.0;

    @Column(name = "final_risk_score", nullable = false)
    private Double finalRiskScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "reason")
    private String reason;

    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.alertUuid == null || this.alertUuid.isBlank()) {
            this.alertUuid = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = AlertStatus.OPEN;
        }

        // Synchronize anomaly_score and ml_score
        if (this.anomalyScore == null && this.mlScore != null) {
            this.anomalyScore = this.mlScore;
        } else if (this.mlScore == null && this.anomalyScore != null) {
            this.mlScore = this.anomalyScore;
        } else if (this.anomalyScore == null) {
            this.anomalyScore = 0.0;
            this.mlScore = 0.0;
        }

        // Synchronize hybrid_score and final_risk_score
        if (this.finalRiskScore == null && this.hybridScore != null) {
            this.finalRiskScore = this.hybridScore;
        } else if (this.hybridScore == null && this.finalRiskScore != null) {
            this.hybridScore = this.finalRiskScore;
        } else if (this.finalRiskScore == null) {
            this.finalRiskScore = 0.0;
            this.hybridScore = 0.0;
        }

        if (this.ruleScore == null) {
            this.ruleScore = 0.0;
        }

        // Synchronize reason and reasons
        if ((this.reason == null || this.reason.isBlank()) && (this.reasons != null && !this.reasons.isBlank())) {
            this.reason = this.reasons;
        } else if ((this.reasons == null || this.reasons.isBlank()) && (this.reason != null && !this.reason.isBlank())) {
            this.reasons = this.reason;
        } else if (this.reason == null || this.reason.isBlank()) {
            this.reason = "AML anomaly threshold exceeded";
            this.reasons = this.reason;
        }

        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.anomalyScore == null && this.mlScore != null) {
            this.anomalyScore = this.mlScore;
        } else if (this.mlScore == null && this.anomalyScore != null) {
            this.mlScore = this.anomalyScore;
        }

        if (this.finalRiskScore == null && this.hybridScore != null) {
            this.finalRiskScore = this.hybridScore;
        } else if (this.hybridScore == null && this.finalRiskScore != null) {
            this.hybridScore = this.finalRiskScore;
        }

        this.updatedAt = OffsetDateTime.now();
    }

    public AmlAlert() {
    }

    public AmlAlert(Customer customer, String transactionUuid, Double ruleScore,
                    Double anomalyScore, Double hybridScore, RiskLevel riskLevel,
                    AlertStatus status, List<String> reasons) {
        this.customer = customer;
        this.transactionUuid = transactionUuid;
        this.ruleScore = ruleScore != null ? ruleScore : 0.0;
        this.anomalyScore = anomalyScore != null ? anomalyScore : 0.0;
        this.mlScore = this.anomalyScore;
        this.hybridScore = hybridScore != null ? hybridScore : 0.0;
        this.finalRiskScore = this.hybridScore;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.HIGH;
        this.status = status != null ? status : AlertStatus.OPEN;
        setReasonsList(reasons);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String alertUuid) {
        this.alertUuid = alertUuid;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
    }

    public Double getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(Double ruleScore) {
        this.ruleScore = ruleScore;
    }

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
        if (this.mlScore == null) {
            this.mlScore = anomalyScore;
        }
    }

    public Double getMlScore() {
        return mlScore;
    }

    public void setMlScore(Double mlScore) {
        this.mlScore = mlScore;
        if (this.anomalyScore == null) {
            this.anomalyScore = mlScore;
        }
    }

    public Double getHybridScore() {
        return hybridScore;
    }

    public void setHybridScore(Double hybridScore) {
        this.hybridScore = hybridScore;
        if (this.finalRiskScore == null) {
            this.finalRiskScore = hybridScore;
        }
    }

    public Double getFinalRiskScore() {
        return finalRiskScore;
    }

    public void setFinalRiskScore(Double finalRiskScore) {
        this.finalRiskScore = finalRiskScore;
        if (this.hybridScore == null) {
            this.hybridScore = finalRiskScore;
        }
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
        this.reason = reasons;
    }

    public List<String> getReasonsList() {
        if (reasons == null || reasons.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(reasons.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void setReasonsList(List<String> reasonsList) {
        if (reasonsList == null || reasonsList.isEmpty()) {
            this.reasons = "";
            this.reason = "AML anomaly threshold exceeded";
        } else {
            this.reasons = String.join(";", reasonsList);
            this.reason = this.reasons;
        }
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
