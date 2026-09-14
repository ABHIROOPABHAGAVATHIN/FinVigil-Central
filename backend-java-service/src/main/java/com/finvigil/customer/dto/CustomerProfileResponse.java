package com.finvigil.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProfileResponse {

    private CustomerResponse customer;
    private Map<String, Object> credit;
    private List<Map<String, Object>> transactions;
    private Map<String, Object> aml;
    private Map<String, Object> riskAggregation;

    public CustomerProfileResponse() {
    }

    public CustomerProfileResponse(CustomerResponse customer, Map<String, Object> credit,
                                   List<Map<String, Object>> transactions, Map<String, Object> aml) {
        this(customer, credit, transactions, aml, null);
    }

    public CustomerProfileResponse(CustomerResponse customer, Map<String, Object> credit,
                                   List<Map<String, Object>> transactions, Map<String, Object> aml,
                                   Map<String, Object> riskAggregation) {
        this.customer = customer;
        this.credit = credit;
        this.transactions = transactions;
        this.aml = aml;
        this.riskAggregation = riskAggregation;
    }

    public CustomerResponse getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerResponse customer) {
        this.customer = customer;
    }

    public Map<String, Object> getCredit() {
        return credit;
    }

    public void setCredit(Map<String, Object> credit) {
        this.credit = credit;
    }

    public List<Map<String, Object>> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Map<String, Object>> transactions) {
        this.transactions = transactions;
    }

    public Map<String, Object> getAml() {
        return aml;
    }

    public void setAml(Map<String, Object> aml) {
        this.aml = aml;
    }

    public Map<String, Object> getRiskAggregation() {
        return riskAggregation;
    }

    public void setRiskAggregation(Map<String, Object> riskAggregation) {
        this.riskAggregation = riskAggregation;
    }
}
