package com.finvigil.customer.dto;

import com.finvigil.common.enums.CustomerStatus;
import java.time.OffsetDateTime;

public class CustomerResponse {

    private String customerUuid;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;
    private OffsetDateTime createdAt;

    public CustomerResponse() {
    }

    public CustomerResponse(String customerUuid, String name, String email, String phone, CustomerStatus status, OffsetDateTime createdAt) {
        this.customerUuid = customerUuid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
