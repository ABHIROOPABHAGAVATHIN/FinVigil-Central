package com.finvigil.aml.model;

import com.finvigil.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionEvaluationContext {

    private final String transactionUuid;
    private final String customerUuid;
    private final BigDecimal amount;
    private final TransactionType transactionType;
    private final String merchant;
    private final String currency;
    private final OffsetDateTime transactionTimestamp;
    private final int velocityCount;
    private final BigDecimal velocityAmount;

    public TransactionEvaluationContext(String transactionUuid, String customerUuid, BigDecimal amount,
                                      TransactionType transactionType, String merchant, String currency,
                                      OffsetDateTime transactionTimestamp, int velocityCount,
                                      BigDecimal velocityAmount) {
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.transactionType = transactionType;
        this.merchant = merchant != null ? merchant : "";
        this.currency = currency != null ? currency : "INR";
        this.transactionTimestamp = transactionTimestamp != null ? transactionTimestamp : OffsetDateTime.now();
        this.velocityCount = velocityCount;
        this.velocityAmount = velocityAmount != null ? velocityAmount : BigDecimal.ZERO;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getVelocityCount() {
        return velocityCount;
    }

    public BigDecimal getVelocityAmount() {
        return velocityAmount;
    }
}
