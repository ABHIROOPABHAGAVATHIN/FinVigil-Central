package com.finvigil.messaging;

import com.finvigil.common.enums.ApplicationStatus;
import com.finvigil.common.enums.AuditAction;
import com.finvigil.config.RabbitMQConfig;
import com.finvigil.credit.entity.CreditApplication;
import com.finvigil.credit.entity.CreditDecision;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.credit.repository.CreditDecisionRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.messaging.dto.CreditDecisionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CreditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreditEventConsumer.class);

    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditDecisionRepository creditDecisionRepository;
    private final AuditLogService auditLogService;

    public CreditEventConsumer(CreditApplicationRepository creditApplicationRepository,
                               CreditDecisionRepository creditDecisionRepository,
                               AuditLogService auditLogService) {
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditDecisionRepository = creditDecisionRepository;
        this.auditLogService = auditLogService;
    }

    @RabbitListener(queues = RabbitMQConfig.CREDIT_DECISION_QUEUE)
    @Transactional
    public void processCreditDecision(CreditDecisionEvent event) {
        log.info("Received credit decision event: decisionUuid={}, appUuid={}, decision={}, riskScore={}, riskLevel={}",
                event.getDecisionUuid(), event.getApplicationUuid(), event.getDecision(),
                event.getRiskScore(), event.getRiskLevel());

        Optional<CreditApplication> appOpt = creditApplicationRepository.findByApplicationUuid(event.getApplicationUuid());
        if (appOpt.isEmpty()) {
            log.error("Credit application not found for decision event: appUuid={}", event.getApplicationUuid());
            return;
        }

        CreditApplication application = appOpt.get();

        CreditDecision decision = new CreditDecision();
        decision.setDecisionUuid(event.getDecisionUuid());
        decision.setApplication(application);
        decision.setRiskScore(event.getRiskScore());
        decision.setRiskLevel(event.getRiskLevel());
        decision.setDecision(event.getDecision());
        decision.setModelVersion(event.getModelVersion());

        creditDecisionRepository.save(decision);

        // Update application status
        switch (event.getDecision()) {
            case APPROVE -> application.setApplicationStatus(ApplicationStatus.APPROVED);
            case REVIEW -> application.setApplicationStatus(ApplicationStatus.REVIEW);
            case REJECT -> application.setApplicationStatus(ApplicationStatus.REJECTED);
        }
        creditApplicationRepository.save(application);

        auditLogService.logEvent(
                "CREDIT",
                "CREDIT_DECISION",
                decision.getDecisionUuid(),
                AuditAction.CREDIT_DECISION_RECEIVED.name()
        );

        log.info("Successfully persisted credit decision: appUuid={}, newStatus={}",
                application.getApplicationUuid(), application.getApplicationStatus());
    }
}
