package com.finvigil.aml.rule;

import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;

import java.util.Optional;

public interface AmlRule {

    String getRuleName();

    AmlRuleType getRuleType();

    String getDescription();

    double getBaseWeight();

    Optional<RuleViolation> evaluate(TransactionEvaluationContext context);
}
