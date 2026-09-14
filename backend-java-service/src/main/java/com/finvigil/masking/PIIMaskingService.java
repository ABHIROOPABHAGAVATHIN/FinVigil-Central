package com.finvigil.masking;

import org.springframework.stereotype.Service;

@Service
public class PIIMaskingService {

    /**
     * Masks an email address: john.doe@example.com -> j***e@example.com
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "******";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username.charAt(0) + "***@" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }

    /**
     * Masks a phone number: 9876543210 -> ******3210
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        String lastFour = phone.substring(phone.length() - 4);
        return "*".repeat(phone.length() - 4) + lastFour;
    }

    /**
     * Masks a customer name: John Doe -> J*** D***
     */
    public String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "******";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() <= 1) {
                sb.append(parts[i]).append("***");
            } else {
                sb.append(parts[i].charAt(0)).append("***");
            }
            if (i < parts.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Masks a PAN / Government ID: ABCDE1234F -> ******1234F
     */
    public String maskGovtId(String id) {
        if (id == null || id.length() <= 5) {
            return "******";
        }
        return "******" + id.substring(id.length() - 5);
    }
}
