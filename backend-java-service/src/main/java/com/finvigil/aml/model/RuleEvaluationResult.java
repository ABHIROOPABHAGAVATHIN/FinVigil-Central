package com.finvigil.aml.model;

import com.finvigil.common.enums.RiskLevel;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class RuleEvaluationResult {

    private final String transactionUuid;
    private final String customerUuid;
    private final double ruleScore;
    private final RiskLevel riskLevel;
    private final boolean alertTriggered;
    private final List<RuleViolation> violations;
    private final List<String> triggeredRules;
    private final OffsetDateTime evaluatedAt;

    public RuleEvaluationResult(String transactionUuid, String customerUuid, double ruleScore,
                                RiskLevel riskLevel, boolean alertTriggered,
                                List<RuleViolation> violations, List<String> triggeredRules,
                                OffsetDateTime evaluatedAt) {
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.ruleScore = Math.min(1.0, Math.max(0.0, ruleScore));
        this.riskLevel = riskLevel;
        this.alertTriggered = alertTriggered;
        this.violations = violations != null ? Collections.unmodifiableList(violations) : Collections.emptyList();
        this.triggeredRules = triggeredRules != null ? Collections.unmodifiableList(triggeredRules) : Collections.emptyList();
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : OffsetDateTime.now();
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public double getRuleScore() {
        return ruleScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public OffsetDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
