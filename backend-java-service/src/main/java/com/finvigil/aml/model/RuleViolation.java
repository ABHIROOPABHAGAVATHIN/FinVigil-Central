package com.finvigil.aml.model;

import com.finvigil.common.enums.RiskLevel;

public class RuleViolation {

    private final AmlRuleType ruleType;
    private final String ruleName;
    private final String description;
    private final RiskLevel severity;
    private final double scoreContribution;
    private final String details;

    public RuleViolation(AmlRuleType ruleType, String ruleName, String description,
                         RiskLevel severity, double scoreContribution, String details) {
        this.ruleType = ruleType;
        this.ruleName = ruleName;
        this.description = description;
        this.severity = severity;
        this.scoreContribution = scoreContribution;
        this.details = details;
    }

    public AmlRuleType getRuleType() {
        return ruleType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDescription() {
        return description;
    }

    public RiskLevel getSeverity() {
        return severity;
    }

    public double getScoreContribution() {
        return scoreContribution;
    }

    public String getDetails() {
        return details;
    }
}
