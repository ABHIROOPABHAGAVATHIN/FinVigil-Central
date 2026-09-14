package com.finvigil.aml.rule;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HighVelocityRule implements AmlRule {

    private final AmlProperties amlProperties;

    public HighVelocityRule(AmlProperties amlProperties) {
        this.amlProperties = amlProperties;
    }

    @Override
    public String getRuleName() {
        return "HIGH_VELOCITY_BURST";
    }

    @Override
    public AmlRuleType getRuleType() {
        return AmlRuleType.HIGH_VELOCITY;
    }

    @Override
    public String getDescription() {
        return "Detects rapid bursts of transactions exceeding the velocity limit (" +
                amlProperties.getVelocityLimit() + " transactions in sliding window).";
    }

    @Override
    public double getBaseWeight() {
        return 0.35;
    }

    @Override
    public Optional<RuleViolation> evaluate(TransactionEvaluationContext context) {
        int limit = amlProperties.getVelocityLimit();
        if (context.getVelocityCount() >= limit) {
            boolean isSevere = context.getVelocityCount() >= (limit * 2);
            RiskLevel severity = isSevere ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            double score = isSevere ? 0.45 : getBaseWeight();

            return Optional.of(new RuleViolation(
                    getRuleType(),
                    getRuleName(),
                    String.format("Customer transaction count (%d) in the sliding window meets or exceeds the velocity limit of %d",
                            context.getVelocityCount(), limit),
                    severity,
                    score,
                    String.format("VelocityCount: %d, Limit: %d, CumulativeAmount: %s",
                            context.getVelocityCount(), limit, context.getVelocityAmount())
            ));
        }
        return Optional.empty();
    }
}
