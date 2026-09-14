package com.finvigil.messaging.dto;

import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;

import java.util.List;

public class AmlAlertEvent {

    private String alertUuid;
    private String transactionUuid;
    private String customerUuid;
    private Double ruleScore;
    private Double mlScore;
    private Double anomalyScore;
    private Double hybridScore;
    private Double finalRiskScore;
    private RiskLevel riskLevel;
    private AlertStatus status;
    private List<String> reasons;
    private String reason;
    private String timestamp;

    public AmlAlertEvent() {
    }

    public AmlAlertEvent(String alertUuid, String transactionUuid, String customerUuid,
                         Double ruleScore, Double mlScore, Double hybridScore,
                         RiskLevel riskLevel, AlertStatus status, List<String> reasons,
                         String timestamp) {
        this.alertUuid = alertUuid;
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.ruleScore = ruleScore;
        this.mlScore = mlScore;
        this.anomalyScore = mlScore;
        this.hybridScore = hybridScore;
        this.finalRiskScore = hybridScore;
        this.riskLevel = riskLevel;
        this.status = status;
        this.reasons = reasons;
        this.timestamp = timestamp;
    }

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String alertUuid) {
        this.alertUuid = alertUuid;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
    }

    public Double getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(Double ruleScore) {
        this.ruleScore = ruleScore;
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

    public Double getAnomalyScore() {
        return anomalyScore != null ? anomalyScore : mlScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
        if (this.mlScore == null) {
            this.mlScore = anomalyScore;
        }
    }

    public Double getEffectiveAnomalyScore() {
        if (this.anomalyScore != null) return this.anomalyScore;
        if (this.mlScore != null) return this.mlScore;
        return 0.0;
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
        return finalRiskScore != null ? finalRiskScore : hybridScore;
    }

    public void setFinalRiskScore(Double finalRiskScore) {
        this.finalRiskScore = finalRiskScore;
        if (this.hybridScore == null) {
            this.hybridScore = finalRiskScore;
        }
    }

    public Double getEffectiveHybridScore() {
        if (this.hybridScore != null) return this.hybridScore;
        if (this.finalRiskScore != null) return this.finalRiskScore;
        return 0.0;
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

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
