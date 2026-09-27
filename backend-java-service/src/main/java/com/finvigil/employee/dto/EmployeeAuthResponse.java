package com.finvigil.employee.dto;

import com.finvigil.common.enums.EmployeeRole;

public class EmployeeAuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String employeeUuid;
    private String email;
    private String name;
    private EmployeeRole role;
    private long expiresIn;

    public EmployeeAuthResponse() {
    }

    public EmployeeAuthResponse(String token, String employeeUuid, String email, String name, EmployeeRole role, long expiresIn) {
        this.token = token;
        this.tokenType = "Bearer";
        this.employeeUuid = employeeUuid;
        this.email = email;
        this.name = name;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getEmployeeUuid() {
        return employeeUuid;
    }

    public void setEmployeeUuid(String employeeUuid) {
        this.employeeUuid = employeeUuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
