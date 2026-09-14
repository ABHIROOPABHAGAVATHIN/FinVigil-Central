package com.finvigil.aml.dto;

import com.finvigil.aml.model.AmlRuleType;

public class RuleDefinitionResponse {

    private String ruleName;
    private AmlRuleType ruleType;
    private String description;
    private double baseWeight;

    public RuleDefinitionResponse() {
    }

    public RuleDefinitionResponse(String ruleName, AmlRuleType ruleType, String description, double baseWeight) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.description = description;
        this.baseWeight = baseWeight;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public AmlRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(AmlRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBaseWeight() {
        return baseWeight;
    }

    public void setBaseWeight(double baseWeight) {
        this.baseWeight = baseWeight;
    }
}
