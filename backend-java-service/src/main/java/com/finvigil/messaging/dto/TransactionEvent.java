package com.finvigil.messaging.dto;

import com.finvigil.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionEvent {

    private String transactionUuid;
    private String customerUuid;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String merchant;
    private OffsetDateTime transactionTimestamp;
    private String currency;
    private Integer velocityCount1m;
    private BigDecimal velocityAmount1m;

    public TransactionEvent() {
    }

    public TransactionEvent(String transactionUuid, String customerUuid, BigDecimal amount,
                            TransactionType transactionType, String merchant,
                            OffsetDateTime transactionTimestamp, String currency,
                            Integer velocityCount1m, BigDecimal velocityAmount1m) {
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.amount = amount;
        this.transactionType = transactionType;
        this.merchant = merchant;
        this.transactionTimestamp = transactionTimestamp;
        this.currency = currency;
        this.velocityCount1m = velocityCount1m;
        this.velocityAmount1m = velocityAmount1m;
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

    public OffsetDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(OffsetDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getVelocityCount1m() {
        return velocityCount1m;
    }

    public void setVelocityCount1m(Integer velocityCount1m) {
        this.velocityCount1m = velocityCount1m;
    }

    public BigDecimal getVelocityAmount1m() {
        return velocityAmount1m;
    }

    public void setVelocityAmount1m(BigDecimal velocityAmount1m) {
        this.velocityAmount1m = velocityAmount1m;
    }
}
