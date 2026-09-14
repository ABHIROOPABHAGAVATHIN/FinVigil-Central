package com.finvigil;

import com.finvigil.common.enums.ApplicationStatus;
import com.finvigil.common.enums.CreditDecisionType;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.config.RabbitMQConfig;
import com.finvigil.credit.entity.CreditApplication;
import com.finvigil.credit.entity.CreditDecision;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.credit.repository.CreditDecisionRepository;
import com.finvigil.customer.repository.AuditLogRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.messaging.CreditEventConsumer;
import com.finvigil.messaging.CreditEventPublisher;
import com.finvigil.messaging.dto.CreditDecisionEvent;
import com.finvigil.messaging.dto.CreditUnderwritingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreditMessagingTest {

    private AmqpTemplate amqpTemplate;
    private CreditEventPublisher creditEventPublisher;
    private CreditApplicationRepository creditApplicationRepository;
    private CreditDecisionRepository creditDecisionRepository;
    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private CreditEventConsumer creditEventConsumer;

    @BeforeEach
    void setUp() {
        amqpTemplate = mock(AmqpTemplate.class);
        creditEventPublisher = new CreditEventPublisher(amqpTemplate);

        creditApplicationRepository = mock(CreditApplicationRepository.class);
        creditDecisionRepository = mock(CreditDecisionRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);
        creditEventConsumer = new CreditEventConsumer(creditApplicationRepository, creditDecisionRepository, auditLogService);
    }

    @Test
    void testPublishCreditUnderwritingEvent() {
        CreditUnderwritingEvent event = new CreditUnderwritingEvent(
                "app-123",
                "cust-456",
                new BigDecimal("60000.00"),
                4,
                new BigDecimal("500000.00"),
                1,
                760,
                new BigDecimal("0.32")
        );

        creditEventPublisher.publishCreditUnderwritingEvent(event);

        verify(amqpTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CREDIT_UNDERWRITING_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void testProcessCreditDecisionEventApprovesApplication() {
        String appUuid = "app-uuid-999";
        CreditApplication application = new CreditApplication();
        application.setApplicationUuid(appUuid);
        application.setApplicationStatus(ApplicationStatus.PENDING);

        when(creditApplicationRepository.findByApplicationUuid(appUuid)).thenReturn(Optional.of(application));

        CreditDecisionEvent decisionEvent = new CreditDecisionEvent(
                UUID.randomUUID().toString(),
                appUuid,
                "cust-456",
                new BigDecimal("0.18"),
                RiskLevel.LOW,
                CreditDecisionType.APPROVE,
                "xgboost-v1"
        );

        creditEventConsumer.processCreditDecision(decisionEvent);

        assertEquals(ApplicationStatus.APPROVED, application.getApplicationStatus());
        verify(creditDecisionRepository, times(1)).save(any(CreditDecision.class));
        verify(creditApplicationRepository, times(1)).save(application);
        verify(auditLogRepository, times(1)).save(any());
    }
}
