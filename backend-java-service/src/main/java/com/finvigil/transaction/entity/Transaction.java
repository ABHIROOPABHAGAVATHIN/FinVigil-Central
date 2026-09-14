package com.finvigil.transaction.entity;

import com.finvigil.common.enums.TransactionStatus;
import com.finvigil.common.enums.TransactionType;
import com.finvigil.customer.entity.Customer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 64)
    private String transactionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @Column(name = "merchant", nullable = false)
    private String merchant;

    @Column(name = "transaction_timestamp", nullable = false)
    private OffsetDateTime transactionTimestamp;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.transactionUuid == null || this.transactionUuid.isBlank()) {
            this.transactionUuid = UUID.randomUUID().toString();
        }
        if (this.transactionTimestamp == null) {
            this.transactionTimestamp = OffsetDateTime.now();
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "INR";
        }
        if (this.status == null) {
            this.status = TransactionStatus.COMPLETED;
        }
        this.createdAt = OffsetDateTime.now();
    }

    public Transaction() {
    }

    public Transaction(Customer customer, BigDecimal amount, TransactionType transactionType,
                       String merchant, String currency, OffsetDateTime transactionTimestamp) {
        this.customer = customer;
        this.amount = amount;
        this.transactionType = transactionType;
        this.merchant = merchant;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "INR";
        this.transactionTimestamp = (transactionTimestamp != null) ? transactionTimestamp : OffsetDateTime.now();
        this.status = TransactionStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
