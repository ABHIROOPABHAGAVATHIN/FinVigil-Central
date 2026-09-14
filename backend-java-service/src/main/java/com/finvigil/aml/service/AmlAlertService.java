package com.finvigil.aml.service;

import com.finvigil.aml.dto.AlertStatusUpdateRequest;
import com.finvigil.aml.dto.AmlAlertResponse;
import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.exception.ResourceNotFoundException;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AmlAlertService {

    private static final Logger log = LoggerFactory.getLogger(AmlAlertService.class);

    private final AmlAlertRepository amlAlertRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public AmlAlertService(AmlAlertRepository amlAlertRepository,
                           CustomerRepository customerRepository,
                           AuditLogService auditLogService) {
        this.amlAlertRepository = amlAlertRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AmlAlert processAlertEvent(AmlAlertEvent event) {
        log.info("Ingesting AML alert event: alertUuid={}, txnUuid={}, custUuid={}, riskLevel={}, hybridScore={}",
                event.getAlertUuid(), event.getTransactionUuid(), event.getCustomerUuid(),
                event.getRiskLevel(), event.getHybridScore());

        // Check if alert already ingested (idempotency)
        if (event.getAlertUuid() != null) {
            var existingOpt = amlAlertRepository.findByAlertUuid(event.getAlertUuid());
            if (existingOpt.isPresent()) {
                log.info("AML alert [{}] already ingested. Skipping duplicate.", event.getAlertUuid());
                return existingOpt.get();
            }
        }

        Customer customer = customerRepository.findByCustomerUuid(event.getCustomerUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerUuid", event.getCustomerUuid()));

        Double ruleScore = event.getRuleScore() != null ? event.getRuleScore() : 0.0;
        Double anomalyScore = event.getEffectiveAnomalyScore();
        Double hybridScore = event.getEffectiveHybridScore();

        AmlAlert alert = new AmlAlert(
                customer,
                event.getTransactionUuid(),
                ruleScore,
                anomalyScore,
                hybridScore,
                event.getRiskLevel() != null ? event.getRiskLevel() : RiskLevel.HIGH,
                event.getStatus() != null ? event.getStatus() : AlertStatus.OPEN,
                event.getReasons()
        );

        if (event.getAlertUuid() != null && !event.getAlertUuid().isBlank()) {
            alert.setAlertUuid(event.getAlertUuid());
        }

        AmlAlert savedAlert = amlAlertRepository.save(alert);

        auditLogService.logEvent(
                "AML",
                "ALERT",
                savedAlert.getAlertUuid(),
                "AML_ALERT_GENERATED"
        );

        log.info("Persisted AML Alert [{}] for customer [{}] with status [{}]",
                savedAlert.getAlertUuid(), customer.getCustomerUuid(), savedAlert.getStatus());

        return savedAlert;
    }

    @Transactional(readOnly = true)
    public AmlAlertResponse getAlertByUuid(String alertUuid) {
        AmlAlert alert = amlAlertRepository.findByAlertUuid(alertUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AmlAlert", "alertUuid", alertUuid));
        return new AmlAlertResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<AmlAlertResponse> getAlertsByCustomer(String customerUuid) {
        return amlAlertRepository.findByCustomer_CustomerUuidOrderByCreatedAtDesc(customerUuid)
                .stream()
                .map(AmlAlertResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmlAlertResponse> getAllAlerts(AlertStatus status, RiskLevel riskLevel) {
        List<AmlAlert> alerts;
        if (status != null && riskLevel != null) {
            alerts = amlAlertRepository.findByStatusOrderByCreatedAtDesc(status)
                    .stream()
                    .filter(a -> a.getRiskLevel() == riskLevel)
                    .collect(Collectors.toList());
        } else if (status != null) {
            alerts = amlAlertRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (riskLevel != null) {
            alerts = amlAlertRepository.findByRiskLevelOrderByCreatedAtDesc(riskLevel);
        } else {
            alerts = amlAlertRepository.findAllByOrderByCreatedAtDesc();
        }

        return alerts.stream()
                .map(AmlAlertResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmlAlertResponse updateAlertStatus(String alertUuid, AlertStatusUpdateRequest request) {
        AmlAlert alert = amlAlertRepository.findByAlertUuid(alertUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AmlAlert", "alertUuid", alertUuid));

        AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(request.getStatus());
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
            alert.setResolutionNotes(request.getResolutionNotes());
        }

        AmlAlert updated = amlAlertRepository.save(alert);

        auditLogService.logEvent(
                "AML",
                "ALERT",
                updated.getAlertUuid(),
                "AML_ALERT_STATUS_CHANGED: " + previousStatus + " -> " + request.getStatus()
        );

        log.info("Updated AML Alert [{}] status from [{}] to [{}]",
                alertUuid, previousStatus, request.getStatus());

        return new AmlAlertResponse(updated);
    }
}
