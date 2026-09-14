package com.finvigil;

import com.finvigil.common.enums.ApplicationStatus;
import com.finvigil.common.enums.CreditDecisionType;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.credit.dto.CreditApplyRequest;
import com.finvigil.credit.dto.CreditApplicationResponse;
import com.finvigil.credit.entity.CreditApplication;
import com.finvigil.credit.entity.CreditDecision;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.credit.repository.CreditDecisionRepository;
import com.finvigil.credit.service.CreditService;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.AuditLogRepository;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.messaging.CreditEventConsumer;
import com.finvigil.messaging.CreditEventPublisher;
import com.finvigil.messaging.dto.CreditDecisionEvent;
import com.finvigil.messaging.dto.CreditUnderwritingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CreditEndToEndIntegrationTest {

    private CreditApplicationRepository creditApplicationRepository;
    private CreditDecisionRepository creditDecisionRepository;
    private CustomerRepository customerRepository;
    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private CreditEventPublisher creditEventPublisher;
    private CreditService creditService;
    private CreditEventConsumer creditEventConsumer;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        creditApplicationRepository = mock(CreditApplicationRepository.class);
        creditDecisionRepository = mock(CreditDecisionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        creditEventPublisher = mock(CreditEventPublisher.class);

        auditLogService = new AuditLogService(auditLogRepository);
        creditService = new CreditService(
                creditApplicationRepository,
                creditDecisionRepository,
                customerRepository,
                auditLogService,
                creditEventPublisher
        );
        creditEventConsumer = new CreditEventConsumer(
                creditApplicationRepository,
                creditDecisionRepository,
                auditLogService
        );

        testCustomer = new Customer(
                "E2E Test User",
                "e2e.user@finvigil.com",
                "+1-555-0999",
                "hashedpassword"
        );
        testCustomer.setId(1L);
        testCustomer.setCustomerUuid(UUID.randomUUID().toString());
        when(customerRepository.findByCustomerUuid(testCustomer.getCustomerUuid()))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(testCustomer));
    }


    @Test
    @DisplayName("Complete End-to-End Underwriting Lifecycle: Submit -> Publish -> Consume Decision -> Persist & Update Status")
    void testCompleteCreditUnderwritingLifecycle() {
        // 1. Submit Credit Application
        CreditApplyRequest request = new CreditApplyRequest(
                testCustomer.getCustomerUuid(),
                BigDecimal.valueOf(110000.00),
                6,
                BigDecimal.valueOf(25000.00),
                1,
                750,
                BigDecimal.valueOf(0.24)
        );

        when(creditApplicationRepository.save(any(CreditApplication.class)))
                .thenAnswer(invocation -> {
                    CreditApplication app = invocation.getArgument(0);
                    app.setId(1L);
                    if (app.getApplicationUuid() == null) {
                        app.setApplicationUuid(UUID.randomUUID().toString());
                    }
                    return app;
                });

        CreditApplicationResponse submissionResponse = creditService.applyForCredit(request);

        assertNotNull(submissionResponse);
        assertNotNull(submissionResponse.getApplicationUuid());
        assertEquals(ApplicationStatus.PENDING, submissionResponse.getApplicationStatus());

        // Verify CreditUnderwritingEvent was published
        ArgumentCaptor<CreditUnderwritingEvent> eventCaptor = ArgumentCaptor.forClass(CreditUnderwritingEvent.class);
        verify(creditEventPublisher, times(1)).publishCreditUnderwritingEvent(eventCaptor.capture());

        CreditUnderwritingEvent publishedEvent = eventCaptor.getValue();
        assertEquals(submissionResponse.getApplicationUuid(), publishedEvent.getApplicationUuid());
        assertEquals(testCustomer.getCustomerUuid(), publishedEvent.getCustomerUuid());
        assertEquals(750, publishedEvent.getCreditScore());

        // 2. Prepare mock for decision consumption
        CreditApplication currentApp = new CreditApplication();
        currentApp.setId(1L);
        currentApp.setApplicationUuid(submissionResponse.getApplicationUuid());
        currentApp.setCustomer(testCustomer);
        currentApp.setApplicationStatus(ApplicationStatus.PENDING);

        when(creditApplicationRepository.findByApplicationUuid(submissionResponse.getApplicationUuid()))
                .thenReturn(Optional.of(currentApp));

        // 3. Simulate Decision Event returned by Python XGBoost ML Worker
        String decisionUuid = UUID.randomUUID().toString();
        CreditDecisionEvent decisionEvent = new CreditDecisionEvent(
                decisionUuid,
                submissionResponse.getApplicationUuid(),
                testCustomer.getCustomerUuid(),
                BigDecimal.valueOf(0.1250),
                RiskLevel.LOW,
                CreditDecisionType.APPROVE,
                "credit_xgboost_v1.0"
        );

        // 4. Process Decision via CreditEventConsumer
        creditEventConsumer.processCreditDecision(decisionEvent);

        // 5. Assert Application Status updated to APPROVED
        assertEquals(ApplicationStatus.APPROVED, currentApp.getApplicationStatus());
        verify(creditApplicationRepository, atLeastOnce()).save(currentApp);

        // 6. Assert CreditDecision record persisted
        ArgumentCaptor<CreditDecision> decisionCaptor = ArgumentCaptor.forClass(CreditDecision.class);
        verify(creditDecisionRepository, times(1)).save(decisionCaptor.capture());
        CreditDecision persistedDecision = decisionCaptor.getValue();
        assertEquals(CreditDecisionType.APPROVE, persistedDecision.getDecision());
        assertEquals(RiskLevel.LOW, persistedDecision.getRiskLevel());
        assertEquals(BigDecimal.valueOf(0.1250), persistedDecision.getRiskScore());
        assertEquals("credit_xgboost_v1.0", persistedDecision.getModelVersion());
        assertEquals(decisionUuid, persistedDecision.getDecisionUuid());
    }

    @Test
    @DisplayName("End-to-End Underwriting Rejection Lifecycle: High Risk Decision -> Status REJECTED")
    void testCreditUnderwritingRejectionLifecycle() {
        CreditApplyRequest request = new CreditApplyRequest(
                testCustomer.getCustomerUuid(),
                BigDecimal.valueOf(25000.00),
                0,
                BigDecimal.valueOf(45000.00),
                5,
                490,
                BigDecimal.valueOf(0.68)
        );

        when(creditApplicationRepository.save(any(CreditApplication.class)))
                .thenAnswer(invocation -> {
                    CreditApplication app = invocation.getArgument(0);
                    app.setId(2L);
                    if (app.getApplicationUuid() == null) {
                        app.setApplicationUuid(UUID.randomUUID().toString());
                    }
                    return app;
                });


        CreditApplicationResponse submissionResponse = creditService.applyForCredit(request);
        assertEquals(ApplicationStatus.PENDING, submissionResponse.getApplicationStatus());

        CreditApplication currentApp = new CreditApplication();
        currentApp.setId(2L);
        currentApp.setApplicationUuid(submissionResponse.getApplicationUuid());
        currentApp.setCustomer(testCustomer);
        currentApp.setApplicationStatus(ApplicationStatus.PENDING);

        when(creditApplicationRepository.findByApplicationUuid(submissionResponse.getApplicationUuid()))
                .thenReturn(Optional.of(currentApp));

        String decisionUuid = UUID.randomUUID().toString();
        CreditDecisionEvent rejectionEvent = new CreditDecisionEvent(
                decisionUuid,
                submissionResponse.getApplicationUuid(),
                testCustomer.getCustomerUuid(),
                BigDecimal.valueOf(0.7820),
                RiskLevel.HIGH,
                CreditDecisionType.REJECT,
                "credit_xgboost_v1.0"
        );

        creditEventConsumer.processCreditDecision(rejectionEvent);

        assertEquals(ApplicationStatus.REJECTED, currentApp.getApplicationStatus());
        verify(creditApplicationRepository, atLeastOnce()).save(currentApp);

        ArgumentCaptor<CreditDecision> decisionCaptor = ArgumentCaptor.forClass(CreditDecision.class);
        verify(creditDecisionRepository, times(1)).save(decisionCaptor.capture());
        assertEquals(CreditDecisionType.REJECT, decisionCaptor.getValue().getDecision());
        assertEquals(RiskLevel.HIGH, decisionCaptor.getValue().getRiskLevel());
    }
}


