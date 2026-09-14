package com.finvigil.credit.dto;

import com.finvigil.common.enums.CreditDecisionType;
import com.finvigil.common.enums.RiskLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CreditDecisionResponse {

    private String decisionUuid;
    private String applicationUuid;
    private BigDecimal riskScore;
    private RiskLevel riskLevel;
    private CreditDecisionType decision;
    private String modelVersion;
    private OffsetDateTime createdAt;

    public CreditDecisionResponse() {
    }

    public CreditDecisionResponse(String decisionUuid, String applicationUuid, BigDecimal riskScore,
                                  RiskLevel riskLevel, CreditDecisionType decision,
                                  String modelVersion, OffsetDateTime createdAt) {
        this.decisionUuid = decisionUuid;
        this.applicationUuid = applicationUuid;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
