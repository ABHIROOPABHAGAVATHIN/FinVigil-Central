package com.finvigil.aml.rule;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class LargeTransactionRule implements AmlRule {

    private final AmlProperties amlProperties;

    public LargeTransactionRule(AmlProperties amlProperties) {
        this.amlProperties = amlProperties;
    }

    @Override
    public String getRuleName() {
        return "LARGE_TRANSACTION_THRESHOLD";
    }

    @Override
    public AmlRuleType getRuleType() {
        return AmlRuleType.LARGE_TRANSACTION;
    }

    @Override
    public String getDescription() {
        return "Detects transactions exceeding mandatory currency transaction reporting threshold (" +
                amlProperties.getLargeTransactionThreshold() + ").";
    }

    @Override
    public double getBaseWeight() {
        return 0.35;
    }

    @Override
    public Optional<RuleViolation> evaluate(TransactionEvaluationContext context) {
        BigDecimal threshold = amlProperties.getLargeTransactionThreshold();
        if (context.getAmount().compareTo(threshold) >= 0) {
            BigDecimal doubleThreshold = threshold.multiply(BigDecimal.valueOf(2));
            boolean isExtreme = context.getAmount().compareTo(doubleThreshold) >= 0;

            RiskLevel severity = isExtreme ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            double score = isExtreme ? 0.45 : getBaseWeight();

            return Optional.of(new RuleViolation(
                    getRuleType(),
                    getRuleName(),
                    "Transaction amount " + context.getAmount() + " " + context.getCurrency() +
                            " meets or exceeds the regulatory threshold of " + threshold,
                    severity,
                    score,
                    String.format("Amount: %s, Threshold: %s, Multiplier: %.2f",
                            context.getAmount(), threshold,
                            context.getAmount().doubleValue() / threshold.doubleValue())
            ));
        }
        return Optional.empty();
    }
}
