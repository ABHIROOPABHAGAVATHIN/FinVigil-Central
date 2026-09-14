package com.finvigil.credit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finvigil.common.enums.ApplicationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditApplicationResponse {

    private String applicationUuid;
    private String customerUuid;
    private BigDecimal income;
    private Integer employmentYears;
    private BigDecimal loanAmount;
    private Integer existingLoans;
    private Integer creditScore;
    private BigDecimal debtToIncomeRatio;
    private ApplicationStatus applicationStatus;
    private OffsetDateTime createdAt;
    private CreditDecisionResponse decision;

    public CreditApplicationResponse() {
    }

    public CreditApplicationResponse(String applicationUuid, String customerUuid, BigDecimal income,
                                     Integer employmentYears, BigDecimal loanAmount,
                                     Integer existingLoans, Integer creditScore,
                                     BigDecimal debtToIncomeRatio, ApplicationStatus applicationStatus,
                                     OffsetDateTime createdAt) {
        this.applicationUuid = applicationUuid;
        this.customerUuid = customerUuid;
        this.income = income;
        this.employmentYears = employmentYears;
        this.loanAmount = loanAmount;
        this.existingLoans = existingLoans;
        this.creditScore = creditScore;
        this.debtToIncomeRatio = debtToIncomeRatio;
        this.applicationStatus = applicationStatus;
        this.createdAt = createdAt;
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

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CreditDecisionResponse getDecision() {
        return decision;
    }

    public void setDecision(CreditDecisionResponse decision) {
        this.decision = decision;
    }
}
