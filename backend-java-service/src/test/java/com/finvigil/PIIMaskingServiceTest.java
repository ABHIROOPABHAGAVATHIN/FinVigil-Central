package com.finvigil;

import com.finvigil.masking.PIIMaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PIIMaskingServiceTest {

    private PIIMaskingService maskingService;

    @BeforeEach
    void setUp() {
        maskingService = new PIIMaskingService();
    }

    @Test
    void testMaskEmail() {
        String masked = maskingService.maskEmail("john.doe@example.com");
        assertTrue(masked.startsWith("j***e@example.com"));
    }

    @Test
    void testMaskPhone() {
        String masked = maskingService.maskPhone("9876543210");
        assertEquals("******3210", masked);
    }

    @Test
    void testMaskName() {
        String masked = maskingService.maskName("John Doe");
        assertEquals("J*** D***", masked);
    }

    @Test
    void testMaskGovtId() {
        String masked = maskingService.maskGovtId("ABCDE1234F");
        assertEquals("******1234F", masked);
    }
}
