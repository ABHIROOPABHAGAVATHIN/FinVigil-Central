package com.finvigil.employee.dto;

import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.common.enums.EmployeeStatus;
import com.finvigil.employee.entity.Employee;

import java.time.OffsetDateTime;

public class EmployeeResponse {

    private String employeeUuid;
    private String name;
    private String email;
    private EmployeeRole role;
    private EmployeeStatus status;
    private OffsetDateTime createdAt;

    public EmployeeResponse() {
    }

    public EmployeeResponse(String employeeUuid, String name, String email, EmployeeRole role, EmployeeStatus status, OffsetDateTime createdAt) {
        this.employeeUuid = employeeUuid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static EmployeeResponse fromEntity(Employee employee) {
        return new EmployeeResponse(
                employee.getEmployeeUuid(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getStatus(),
                employee.getCreatedAt()
        );
    }

    public String getEmployeeUuid() {
        return employeeUuid;
    }

    public void setEmployeeUuid(String employeeUuid) {
        this.employeeUuid = employeeUuid;
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

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
