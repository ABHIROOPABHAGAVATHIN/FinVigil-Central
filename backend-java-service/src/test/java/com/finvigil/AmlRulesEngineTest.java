package com.finvigil;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleEvaluationResult;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.aml.rule.*;
import com.finvigil.aml.service.AmlRulesEngine;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.common.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AmlRulesEngineTest {

    private AmlRulesEngine rulesEngine;
    private AmlProperties amlProperties;

    @BeforeEach
    void setUp() {
        amlProperties = new AmlProperties();
        amlProperties.setLargeTransactionThreshold(BigDecimal.valueOf(50000.0));
        amlProperties.setVelocityLimit(5);
        amlProperties.setVelocityWindowSeconds(60);
        amlProperties.setAlertThreshold(0.65);

        List<AmlRule> rules = List.of(
                new LargeTransactionRule(amlProperties),
                new StructuringRule(amlProperties),
                new HighVelocityRule(amlProperties),
                new HighRiskMerchantRule(amlProperties),
                new RapidOutflowRule(amlProperties)
        );

        rulesEngine = new AmlRulesEngine(rules, amlProperties);
    }

    @Test
    void testCleanTransactionPassesWithLowRisk() {
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-1", "cust-1",
                BigDecimal.valueOf(250.0),
                TransactionType.PURCHASE,
                "Starbucks Coffee",
                "INR",
                OffsetDateTime.now(),
                1,
                BigDecimal.valueOf(250.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertEquals(0.0, result.getRuleScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertFalse(result.isAlertTriggered());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void testLargeTransactionTriggersRule() {
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-2", "cust-2",
                BigDecimal.valueOf(75000.0),
                TransactionType.TRANSFER,
                "Standard Bank",
                "INR",
                OffsetDateTime.now(),
                1,
                BigDecimal.valueOf(75000.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertTrue(result.getRuleScore() >= 0.35);
        assertTrue(result.getTriggeredRules().contains("LARGE_TRANSACTION_THRESHOLD"));
        assertEquals(1, result.getViolations().size());
        assertEquals(AmlRuleType.LARGE_TRANSACTION, result.getViolations().get(0).getRuleType());
    }

    @Test
    void testStructuringDetectionRule() {
        // 48,000 is in [40,000 - 50,000], intentionally below 50,000 CTR limit
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-3", "cust-3",
                BigDecimal.valueOf(48000.0),
                TransactionType.DEPOSIT,
                "Local Branch Cash",
                "INR",
                OffsetDateTime.now(),
                2,
                BigDecimal.valueOf(96000.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertTrue(result.getTriggeredRules().contains("STRUCTURING_DETECTION"));
        assertTrue(result.getRuleScore() >= 0.40);
        assertTrue(result.getViolations().stream()
                .anyMatch(v -> v.getRuleType() == AmlRuleType.STRUCTURING));
    }

    @Test
    void testHighVelocityBurstRule() {
        // 7 transactions in sliding window, exceeding limit of 5
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-4", "cust-4",
                BigDecimal.valueOf(100.0),
                TransactionType.PURCHASE,
                "Retail Store",
                "INR",
                OffsetDateTime.now(),
                7,
                BigDecimal.valueOf(700.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertTrue(result.getTriggeredRules().contains("HIGH_VELOCITY_BURST"));
        assertTrue(result.getViolations().stream()
                .anyMatch(v -> v.getRuleType() == AmlRuleType.HIGH_VELOCITY));
    }

    @Test
    void testHighRiskMerchantRule() {
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-5", "cust-5",
                BigDecimal.valueOf(500.0),
                TransactionType.PURCHASE,
                "Offshore Crypto Exchange Ltd",
                "INR",
                OffsetDateTime.now(),
                1,
                BigDecimal.valueOf(500.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertTrue(result.getTriggeredRules().contains("HIGH_RISK_MERCHANT_CATEGORY"));
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertTrue(result.isAlertTriggered());
    }

    @Test
    void testCombinedRiskTriggersAlert() {
        // Large amount + High-risk merchant + High velocity
        TransactionEvaluationContext context = new TransactionEvaluationContext(
                "txn-6", "cust-6",
                BigDecimal.valueOf(150000.0),
                TransactionType.TRANSFER,
                "Darknet Crypto Mixer Global",
                "INR",
                OffsetDateTime.now(),
                6,
                BigDecimal.valueOf(250000.0)
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);

        assertTrue(result.getRuleScore() >= 0.70);
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertTrue(result.isAlertTriggered());
        assertTrue(result.getViolations().size() >= 3);
    }
}
