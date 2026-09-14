package com.finvigil.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finvigil.common.enums.TransactionStatus;
import com.finvigil.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private String transactionUuid;
    private String customerUuid;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String merchant;
    private OffsetDateTime transactionTimestamp;
    private String currency;
    private TransactionStatus status;
    private Integer velocityCount;
    private BigDecimal velocityAmount;
    private OffsetDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(String transactionUuid, String customerUuid, BigDecimal amount,
                               TransactionType transactionType, String merchant,
                               OffsetDateTime transactionTimestamp, String currency,
                               TransactionStatus status, Integer velocityCount,
                               BigDecimal velocityAmount, OffsetDateTime createdAt) {
        this.transactionUuid = transactionUuid;
        this.customerUuid = customerUuid;
        this.amount = amount;
        this.transactionType = transactionType;
        this.merchant = merchant;
        this.transactionTimestamp = transactionTimestamp;
        this.currency = currency;
        this.status = status;
        this.velocityCount = velocityCount;
        this.velocityAmount = velocityAmount;
        this.createdAt = createdAt;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Integer getVelocityCount() {
        return velocityCount;
    }

    public void setVelocityCount(Integer velocityCount) {
        this.velocityCount = velocityCount;
    }

    public BigDecimal getVelocityAmount() {
        return velocityAmount;
    }

    public void setVelocityAmount(BigDecimal velocityAmount) {
        this.velocityAmount = velocityAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
