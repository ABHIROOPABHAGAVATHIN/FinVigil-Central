package com.finvigil;

import com.finvigil.aml.dto.AlertStatusUpdateRequest;
import com.finvigil.aml.dto.AmlAlertResponse;
import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.CustomerStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.exception.ResourceNotFoundException;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmlAlertServiceTest {

    @Mock
    private AmlAlertRepository amlAlertRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AmlAlertService amlAlertService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer("John Doe", "john@example.com", "+1234567890", "hash");
        sampleCustomer.setCustomerUuid("cust-uuid-123");
        sampleCustomer.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    void testProcessAlertEventSuccess() {
        AmlAlertEvent event = new AmlAlertEvent(
                "alert-uuid-001",
                "txn-uuid-001",
                "cust-uuid-123",
                0.45,
                0.72,
                0.612,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("LARGE_TRANSACTION_THRESHOLD", "HIGH_VELOCITY_BURST"),
                "2026-09-14T19:00:00Z"
        );

        when(amlAlertRepository.findByAlertUuid("alert-uuid-001")).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerUuid("cust-uuid-123")).thenReturn(Optional.of(sampleCustomer));

        AmlAlert savedMock = new AmlAlert(
                sampleCustomer,
                "txn-uuid-001",
                0.45, 0.72, 0.612,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                event.getReasons()
        );
        ArgumentCaptor<AmlAlert> alertCaptor = ArgumentCaptor.forClass(AmlAlert.class);
        when(amlAlertRepository.save(alertCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        AmlAlert result = amlAlertService.processAlertEvent(event);

        assertNotNull(result);
        assertEquals("alert-uuid-001", result.getAlertUuid());
        assertEquals("cust-uuid-123", result.getCustomer().getCustomerUuid());
        assertEquals("txn-uuid-001", result.getTransactionUuid());
        assertEquals(0.45, result.getRuleScore());
        assertEquals(0.72, result.getAnomalyScore());
        assertEquals(0.72, result.getMlScore());
        assertEquals(0.612, result.getHybridScore());
        assertEquals(0.612, result.getFinalRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(AlertStatus.OPEN, result.getStatus());
        assertNotNull(result.getAnomalyScore(), "anomaly_score must not be null");

        AmlAlert captured = alertCaptor.getValue();
        assertEquals(0.72, captured.getAnomalyScore());
        assertEquals(0.612, captured.getHybridScore());
        assertEquals(0.45, captured.getRuleScore());
        assertEquals("txn-uuid-001", captured.getTransactionUuid());

        verify(auditLogService, times(1)).logEvent(eq("AML"), eq("ALERT"), eq("alert-uuid-001"), eq("AML_ALERT_GENERATED"));
    }

    @Test
    void testUpdateAlertStatus() {
        AmlAlert alert = new AmlAlert(
                sampleCustomer,
                "txn-1",
                0.4, 0.6, 0.52,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("SUSPICIOUS")
        );
        alert.setAlertUuid("alert-123");

        when(amlAlertRepository.findByAlertUuid("alert-123")).thenReturn(Optional.of(alert));
        when(amlAlertRepository.save(any(AmlAlert.class))).thenAnswer(i -> i.getArgument(0));

        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.RESOLVED, "Investigated and verified legitimate source of funds");
        AmlAlertResponse response = amlAlertService.updateAlertStatus("alert-123", request);

        assertNotNull(response);
        assertEquals(AlertStatus.RESOLVED, response.getStatus());
        assertEquals("Investigated and verified legitimate source of funds", response.getResolutionNotes());

        verify(auditLogService, times(1)).logEvent(eq("AML"), eq("ALERT"), eq("alert-123"), contains("AML_ALERT_STATUS_CHANGED"));
    }

    @Test
    void testGetAlertByUuidNotFoundThrowsException() {
        when(amlAlertRepository.findByAlertUuid("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> amlAlertService.getAlertByUuid("non-existent"));
    }
}
