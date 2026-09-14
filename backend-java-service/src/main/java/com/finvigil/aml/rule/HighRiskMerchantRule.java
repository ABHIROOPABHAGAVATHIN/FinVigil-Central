package com.finvigil.aml.rule;

import com.finvigil.aml.config.AmlProperties;
import com.finvigil.aml.model.AmlRuleType;
import com.finvigil.aml.model.RuleViolation;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class HighRiskMerchantRule implements AmlRule {

    private final AmlProperties amlProperties;

    public HighRiskMerchantRule(AmlProperties amlProperties) {
        this.amlProperties = amlProperties;
    }

    @Override
    public String getRuleName() {
        return "HIGH_RISK_MERCHANT_CATEGORY";
    }

    @Override
    public AmlRuleType getRuleType() {
        return AmlRuleType.HIGH_RISK_MERCHANT;
    }

    @Override
    public String getDescription() {
        return "Identifies transactions associated with high-risk industries (e.g. gambling, cryptocurrency, binary options, mixers).";
    }

    @Override
    public double getBaseWeight() {
        return 0.40;
    }

    @Override
    public Optional<RuleViolation> evaluate(TransactionEvaluationContext context) {
        String merchant = context.getMerchant() != null ? context.getMerchant().toLowerCase(Locale.ROOT) : "";

        for (String keyword : amlProperties.getHighRiskKeywords()) {
            if (merchant.contains(keyword.toLowerCase(Locale.ROOT))) {
                return Optional.of(new RuleViolation(
                        getRuleType(),
                        getRuleName(),
                        String.format("Merchant '%s' matches high-risk AML watch-list keyword '%s'",
                                context.getMerchant(), keyword),
                        RiskLevel.HIGH,
                        getBaseWeight(),
                        String.format("MatchedKeyword: %s, RawMerchant: %s", keyword, context.getMerchant())
                ));
            }
        }
        return Optional.empty();
    }
}
