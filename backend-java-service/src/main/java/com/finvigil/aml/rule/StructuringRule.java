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
public class StructuringRule implements AmlRule {

    private final AmlProperties amlProperties;

    public StructuringRule(AmlProperties amlProperties) {
        this.amlProperties = amlProperties;
    }

    @Override
    public String getRuleName() {
        return "STRUCTURING_DETECTION";
    }

    @Override
    public AmlRuleType getRuleType() {
        return AmlRuleType.STRUCTURING;
    }

    @Override
    public String getDescription() {
        return "Detects deliberate smurfing / structuring patterns designed to circumvent reporting thresholds.";
    }

    @Override
    public double getBaseWeight() {
        return 0.40;
    }

    @Override
    public Optional<RuleViolation> evaluate(TransactionEvaluationContext context) {
        BigDecimal threshold = amlProperties.getLargeTransactionThreshold();
        BigDecimal lowerBand = threshold.multiply(BigDecimal.valueOf(0.80)); // 80% of threshold

        boolean isJustBelowThreshold = context.getAmount().compareTo(lowerBand) >= 0 &&
                context.getAmount().compareTo(threshold) < 0;

        boolean isSubThresholdAccumulation = context.getVelocityCount() >= 2 &&
                context.getAmount().compareTo(threshold) < 0 &&
                context.getVelocityAmount().compareTo(threshold) >= 0;

        if (isJustBelowThreshold || isSubThresholdAccumulation) {
            String reason = isJustBelowThreshold
                    ? String.format("Transaction amount %s is in the critical structuring corridor [80%% - 100%%] of threshold %s",
                    context.getAmount(), threshold)
                    : String.format("Cumulative velocity amount %s across %d transactions exceeds threshold %s while single amount %s is sub-threshold",
                    context.getVelocityAmount(), context.getVelocityCount(), threshold, context.getAmount());

            RiskLevel severity = (isJustBelowThreshold && context.getVelocityCount() >= 2) ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            double score = severity == RiskLevel.HIGH ? 0.45 : getBaseWeight();

            return Optional.of(new RuleViolation(
                    getRuleType(),
                    getRuleName(),
                    reason,
                    severity,
                    score,
                    String.format("Amount: %s, VelocityCount: %d, VelocityTotal: %s, Threshold: %s",
                            context.getAmount(), context.getVelocityCount(), context.getVelocityAmount(), threshold)
            ));
        }

        return Optional.empty();
    }
}
