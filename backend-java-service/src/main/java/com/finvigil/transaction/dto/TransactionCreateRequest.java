package com.finvigil.transaction.dto;

import com.finvigil.common.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionCreateRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotBlank(message = "Merchant is required")
    private String merchant;

    private String currency = "INR";

    private OffsetDateTime transactionTimestamp;

    public TransactionCreateRequest() {
    }

    public TransactionCreateRequest(String customerId, BigDecimal amount, TransactionType transactionType,
                                  String merchant, String currency, OffsetDateTime transactionTimestamp) {
        this.customerId = customerId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.merchant = merchant;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "INR";
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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
}
