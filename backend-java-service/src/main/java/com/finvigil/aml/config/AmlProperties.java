package com.finvigil.aml.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.aml")
public class AmlProperties {

    private BigDecimal largeTransactionThreshold = BigDecimal.valueOf(50000.0);
    private int velocityLimit = 5;
    private int velocityWindowSeconds = 60;
    private double ruleWeight = 0.4;
    private double mlWeight = 0.6;
    private double alertThreshold = 0.65;
    private List<String> highRiskKeywords = List.of(
            "casino", "gambling", "betting", "crypto", "bitcoin", "darknet",
            "mixer", "offshore", "forex", "binary", "unregulated"
    );

    public BigDecimal getLargeTransactionThreshold() {
        return largeTransactionThreshold;
    }

    public void setLargeTransactionThreshold(BigDecimal largeTransactionThreshold) {
        this.largeTransactionThreshold = largeTransactionThreshold;
    }

    public int getVelocityLimit() {
        return velocityLimit;
    }

    public void setVelocityLimit(int velocityLimit) {
        this.velocityLimit = velocityLimit;
    }

    public int getVelocityWindowSeconds() {
        return velocityWindowSeconds;
    }

    public void setVelocityWindowSeconds(int velocityWindowSeconds) {
        this.velocityWindowSeconds = velocityWindowSeconds;
    }

    public double getRuleWeight() {
        return ruleWeight;
    }

    public void setRuleWeight(double ruleWeight) {
        this.ruleWeight = ruleWeight;
    }

    public double getMlWeight() {
        return mlWeight;
    }

    public void setMlWeight(double mlWeight) {
        this.mlWeight = mlWeight;
    }

    public double getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(double alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public List<String> getHighRiskKeywords() {
        return highRiskKeywords;
    }

    public void setHighRiskKeywords(List<String> highRiskKeywords) {
        this.highRiskKeywords = highRiskKeywords;
    }
}
