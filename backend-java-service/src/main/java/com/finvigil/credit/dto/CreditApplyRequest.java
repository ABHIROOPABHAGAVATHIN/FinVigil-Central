package com.finvigil.credit.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreditApplyRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Income is required")
    @Positive(message = "Income must be greater than zero")
    private BigDecimal income;

    @NotNull(message = "Employment years is required")
    @Min(value = 0, message = "Employment years must be zero or positive")
    @Max(value = 60, message = "Employment years cannot exceed 60")
    private Integer employmentYears;

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be greater than zero")
    private BigDecimal loanAmount;

    @NotNull(message = "Existing loans count is required")
    @Min(value = 0, message = "Existing loans count must be zero or positive")
    private Integer existingLoans;

    @NotNull(message = "Credit score is required")
    @Min(value = 300, message = "Credit score must be between 300 and 850")
    @Max(value = 850, message = "Credit score must be between 300 and 850")
    private Integer creditScore;

    @NotNull(message = "Debt-to-income ratio is required")
    @DecimalMin(value = "0.0", message = "Debt-to-income ratio must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Debt-to-income ratio must be between 0.0 and 1.0")
    private BigDecimal debtToIncomeRatio;

    public CreditApplyRequest() {
    }

    public CreditApplyRequest(String customerId, BigDecimal income, Integer employmentYears,
                              BigDecimal loanAmount, Integer existingLoans, Integer creditScore,
                              BigDecimal debtToIncomeRatio) {
        this.customerId = customerId;
        this.income = income;
        this.employmentYears = employmentYears;
        this.loanAmount = loanAmount;
        this.existingLoans = existingLoans;
        this.creditScore = creditScore;
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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
}
