package com.finvigil.aml.service;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.RuleEvaluationResult;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.aml.rule.AmlRule;
import com.finvigil.common.enums.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AmlRulesEngine {

    private static final Logger log = LoggerFactory.getLogger(AmlRulesEngine.class);

    private final List<AmlRule> rules;
    private final AmlProperties amlProperties;

    public AmlRulesEngine(List<AmlRule> rules, AmlProperties amlProperties) {
        this.rules = rules;
        this.amlProperties = amlProperties;
        log.info("Initialized AML Rules Engine with {} active compliance rules.", rules.size());
    }

    public RuleEvaluationResult evaluateTransaction(TransactionEvaluationContext context) {
        List<RuleViolation> violations = new ArrayList<>();

        for (AmlRule rule : rules) {
            try {
                Optional<RuleViolation> violationOpt = rule.evaluate(context);
                violationOpt.ifPresent(violations::add);
            } catch (Exception ex) {
                log.error("Error executing AML rule [{}] on transaction [{}]:",
                        rule.getRuleName(), context.getTransactionUuid(), ex);
            }
        }

        // Calculate probabilistic risk score: 1 - Product(1 - score_i)
        double cumulativeProbNotRisky = 1.0;
        boolean hasHighSeverity = false;

        for (RuleViolation violation : violations) {
            cumulativeProbNotRisky *= (1.0 - Math.min(0.95, violation.getScoreContribution()));
            if (violation.getSeverity() == RiskLevel.HIGH) {
                hasHighSeverity = true;
            }
        }

        double calculatedScore = violations.isEmpty() ? 0.0 : (1.0 - cumulativeProbNotRisky);
        calculatedScore = Math.round(calculatedScore * 1000.0) / 1000.0; // 3 decimal places

        // Determine categorical RiskLevel
        RiskLevel riskLevel;
        if (calculatedScore >= 0.70 || (hasHighSeverity && calculatedScore >= 0.40)) {
            riskLevel = RiskLevel.HIGH;
        } else if (calculatedScore >= 0.30 || !violations.isEmpty()) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }

        // Determine if alert should be dispatched
        boolean alertTriggered = calculatedScore >= amlProperties.getAlertThreshold() ||
                (hasHighSeverity && calculatedScore >= 0.40);

        List<String> triggeredRuleNames = violations.stream()
                .map(RuleViolation::getRuleName)
                .collect(Collectors.toList());

        log.debug("Evaluated txn [{}]: score={}, risk={}, triggeredRules={}",
                context.getTransactionUuid(), calculatedScore, riskLevel, triggeredRuleNames);

        return new RuleEvaluationResult(
                context.getTransactionUuid(),
                context.getCustomerUuid(),
                calculatedScore,
                riskLevel,
                alertTriggered,
                violations,
                triggeredRuleNames,
                OffsetDateTime.now()
        );
    }

    public List<AmlRule> getActiveRules() {
        return List.copyOf(rules);
    }
}
