package com.finvigil.auth.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String customerUuid;
    private String email;
    private String name;
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String token, String customerUuid, String email, String name, long expiresIn) {
        this.token = token;
        this.tokenType = "Bearer";
        this.customerUuid = customerUuid;
        this.email = email;
        this.name = name;
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

    public String getCustomerUuid() {
        return customerUuid;
    }

    public void setCustomerUuid(String customerUuid) {
        this.customerUuid = customerUuid;
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

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
