package com.finvigil.aml.rule;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.common.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class RapidOutflowRule implements AmlRule {

    private final AmlProperties amlProperties;

    public RapidOutflowRule(AmlProperties amlProperties) {
        this.amlProperties = amlProperties;
    }

    @Override
    public String getRuleName() {
        return "RAPID_OUTFLOW_DRAIN";
    }

    @Override
    public AmlRuleType getRuleType() {
        return AmlRuleType.RAPID_OUTFLOW;
    }

    @Override
    public String getDescription() {
        return "Detects immediate rapid funds drain via transfer or withdrawal following bursts of activity.";
    }

    @Override
    public double getBaseWeight() {
        return 0.30;
    }

    @Override
    public Optional<RuleViolation> evaluate(TransactionEvaluationContext context) {
        boolean isOutflow = context.getTransactionType() == TransactionType.WITHDRAWAL ||
                context.getTransactionType() == TransactionType.TRANSFER;

        if (!isOutflow) {
            return Optional.empty();
        }

        // Check if rapid outflow: high amount coupled with rapid velocity (>= 3 transactions)
        BigDecimal threshold = amlProperties.getLargeTransactionThreshold().multiply(BigDecimal.valueOf(0.50));
        boolean isSignificantAmount = context.getAmount().compareTo(threshold) >= 0;
        boolean hasFrequentPriorActions = context.getVelocityCount() >= 3;

        if (isSignificantAmount && hasFrequentPriorActions) {
            return Optional.of(new RuleViolation(
                    getRuleType(),
                    getRuleName(),
                    String.format("Rapid capital outflow detected: %s of %s %s with %d transactions in the active window",
                            context.getTransactionType(), context.getAmount(), context.getCurrency(), context.getVelocityCount()),
                    RiskLevel.MEDIUM,
                    getBaseWeight(),
                    String.format("Type: %s, Amount: %s, VelocityCount: %d",
                            context.getTransactionType(), context.getAmount(), context.getVelocityCount())
            ));
        }

        return Optional.empty();
    }
}
