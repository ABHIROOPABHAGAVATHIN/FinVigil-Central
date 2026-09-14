package com.finvigil.aml.dto;

import com.finvigil.common.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class RuleEvaluationRequest {

    private String transactionUuid;
    private String customerUuid;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    private String merchant;
    private String currency = "INR";
    private OffsetDateTime transactionTimestamp;
    private int velocityCount;
    private BigDecimal velocityAmount;

    public RuleEvaluationRequest() {
    }

    public RuleEvaluationRequest(String transactionUuid, String customerUuid, BigDecimal amount,
                                TransactionType transactionType, String merchant, String currency,
                                OffsetDateTime transactionTimestamp, int velocityCount, BigDecimal velocityAmount) {
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.amount = amount;
        this.transactionType = transactionType;
        this.merchant = merchant;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "INR";
        this.transactionTimestamp = transactionTimestamp;
        this.velocityCount = velocityCount;
        this.velocityAmount = velocityAmount;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OffsetDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(OffsetDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public int getVelocityCount() {
        return velocityCount;
    }

    public void setVelocityCount(int velocityCount) {
        this.velocityCount = velocityCount;
    }

    public BigDecimal getVelocityAmount() {
        return velocityAmount;
    }

    public void setVelocityAmount(BigDecimal velocityAmount) {
        this.velocityAmount = velocityAmount;
    }
}
