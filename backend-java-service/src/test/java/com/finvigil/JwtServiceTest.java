package com.finvigil;

import com.finvigil.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testTokenGenerationAndValidation() {
        String email = "analyst@finvigil.com";
        String customerUuid = "cust-uuid-1234-5678";

        String token = jwtService.generateToken(email, customerUuid);

        assertTrue(jwtService.validateToken(token));
        assertEquals(email, jwtService.extractUsername(token));
        assertEquals(customerUuid, jwtService.extractCustomerUuid(token));
        assertTrue(jwtService.isTokenValid(token, email));
    }
}
