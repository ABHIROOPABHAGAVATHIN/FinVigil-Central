package com.finvigil.messaging.dto;

import com.finvigil.common.enums.CreditDecisionType;
import com.finvigil.common.enums.RiskLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Credit Decision Event returned from Python XGBoost model worker.
 */
public class CreditDecisionEvent {

    private String decisionUuid;
    private String applicationUuid;
    private String customerUuid;
    private BigDecimal riskScore;
    private RiskLevel riskLevel;
    private CreditDecisionType decision;
    private String modelVersion;
    private OffsetDateTime timestamp;

    public CreditDecisionEvent() {
    }

    public CreditDecisionEvent(String decisionUuid, String applicationUuid, String customerUuid,
                               BigDecimal riskScore, RiskLevel riskLevel,
                               CreditDecisionType decision, String modelVersion) {
        this.decisionUuid = decisionUuid;
        this.applicationUuid = applicationUuid;
        this.customerUuid = customerUuid;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.modelVersion = modelVersion;
        this.timestamp = OffsetDateTime.now();
    }

    public String getDecisionUuid() {
        return decisionUuid;
    }

    public void setDecisionUuid(String decisionUuid) {
        this.decisionUuid = decisionUuid;
    }

    public String getApplicationUuid() {
        return applicationUuid;
    }

    public void setApplicationUuid(String applicationUuid) {
        this.applicationUuid = applicationUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public CreditDecisionType getDecision() {
        return decision;
    }

    public void setDecision(CreditDecisionType decision) {
        this.decision = decision;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
