package com.finvigil.aml.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finvigil.aml.model.RuleEvaluationResult;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.common.enums.RiskLevel;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleEvaluationResponse {

    private String transactionUuid;
    private String customerUuid;
    private double ruleScore;
    private RiskLevel riskLevel;
    private boolean alertTriggered;
    private List<RuleViolation> violations;
    private List<String> triggeredRules;
    private OffsetDateTime evaluatedAt;

    public RuleEvaluationResponse() {
    }

    public RuleEvaluationResponse(RuleEvaluationResult result) {
        this.transactionUuid = result.getTransactionUuid();
        this.customerUuid = result.getCustomerUuid();
        this.ruleScore = result.getRuleScore();
        this.riskLevel = result.getRiskLevel();
        this.alertTriggered = result.isAlertTriggered();
        this.violations = result.getViolations();
        this.triggeredRules = result.getTriggeredRules();
        this.evaluatedAt = result.getEvaluatedAt();
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

    public double getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(double ruleScore) {
        this.ruleScore = ruleScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public void setAlertTriggered(boolean alertTriggered) {
        this.alertTriggered = alertTriggered;
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }

    public void setViolations(List<RuleViolation> violations) {
        this.violations = violations;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<String> triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public OffsetDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(OffsetDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
