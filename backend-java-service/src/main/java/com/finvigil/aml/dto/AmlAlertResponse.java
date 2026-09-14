package com.finvigil.aml.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmlAlertResponse {

    private String alertUuid;
    private String customerUuid;
    private String transactionUuid;
    private Double ruleScore;
    private Double anomalyScore;
    private Double mlScore;
    private Double hybridScore;
    private RiskLevel riskLevel;
    private AlertStatus status;
    private List<String> reasons;
    private String resolutionNotes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AmlAlertResponse() {
    }

    public AmlAlertResponse(AmlAlert alert) {
        this.alertUuid = alert.getAlertUuid();
        this.customerUuid = alert.getCustomer() != null ? alert.getCustomer().getCustomerUuid() : null;
        this.transactionUuid = alert.getTransactionUuid();
        this.ruleScore = alert.getRuleScore();
        this.anomalyScore = alert.getAnomalyScore();
        this.mlScore = alert.getMlScore();
        this.hybridScore = alert.getHybridScore();
        this.riskLevel = alert.getRiskLevel();
        this.status = alert.getStatus();
        this.reasons = alert.getReasonsList();
        this.resolutionNotes = alert.getResolutionNotes();
        this.createdAt = alert.getCreatedAt();
        this.updatedAt = alert.getUpdatedAt();
    }

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String alertUuid) {
        this.alertUuid = alertUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
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
    }

    public Double getMlScore() {
        return mlScore;
    }

    public void setMlScore(Double mlScore) {
        this.mlScore = mlScore;
    }

    public Double getHybridScore() {
        return hybridScore;
    }

    public void setHybridScore(Double hybridScore) {
        this.hybridScore = hybridScore;
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
