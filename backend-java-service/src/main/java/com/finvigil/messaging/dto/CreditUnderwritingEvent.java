package com.finvigil.messaging.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Privacy-Preserving Credit Underwriting Event.
 * Contains only non-PII numerical/business features for ML risk scoring.
 */
public class CreditUnderwritingEvent {

    private String applicationUuid;
    private String customerUuid;
    private BigDecimal income;
    private Integer employmentYears;
    private BigDecimal loanAmount;
    private Integer existingLoans;
    private Integer creditScore;
    private BigDecimal debtToIncomeRatio;
    private OffsetDateTime timestamp;

    public CreditUnderwritingEvent() {
    }

    public CreditUnderwritingEvent(String applicationUuid, String customerUuid, BigDecimal income,
                                   Integer employmentYears, BigDecimal loanAmount,
                                   Integer existingLoans, Integer creditScore,
                                   BigDecimal debtToIncomeRatio) {
        this.applicationUuid = applicationUuid;
        this.customerUuid = customerUuid;
        this.income = income;
        this.employmentYears = employmentYears;
        this.loanAmount = loanAmount;
        this.existingLoans = existingLoans;
        this.creditScore = creditScore;
        this.debtToIncomeRatio = debtToIncomeRatio;
        this.timestamp = OffsetDateTime.now();
    }

    public String getApplicationUuid() {
        return applicationUuid;
    }

    public void setApplicationUuid(String applicationUuid) {
        this.applicationUuid = applicationUuid;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public Integer getEmploymentYears() {
        return employmentYears;
    }

    public void setEmploymentYears(Integer employmentYears) {
        this.employmentYears = employmentYears;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getExistingLoans() {
        return existingLoans;
    }

    public void setExistingLoans(Integer existingLoans) {
        this.existingLoans = existingLoans;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public BigDecimal getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(BigDecimal debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
