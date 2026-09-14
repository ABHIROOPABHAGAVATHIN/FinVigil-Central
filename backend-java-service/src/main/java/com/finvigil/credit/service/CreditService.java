package com.finvigil.credit.service;

import com.finvigil.common.enums.ApplicationStatus;
import com.finvigil.common.enums.AuditAction;
import com.finvigil.credit.dto.CreditApplicationResponse;
import com.finvigil.credit.dto.CreditApplyRequest;
import com.finvigil.credit.dto.CreditDecisionResponse;
import com.finvigil.credit.entity.CreditApplication;
import com.finvigil.credit.entity.CreditDecision;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.credit.repository.CreditDecisionRepository;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CreditService {

    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditDecisionRepository creditDecisionRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final com.finvigil.messaging.CreditEventPublisher creditEventPublisher;

    public CreditService(CreditApplicationRepository creditApplicationRepository,
                         CreditDecisionRepository creditDecisionRepository,
                         CustomerRepository customerRepository,
                         AuditLogService auditLogService,
                         com.finvigil.messaging.CreditEventPublisher creditEventPublisher) {
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditDecisionRepository = creditDecisionRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.creditEventPublisher = creditEventPublisher;
    }

    @Transactional
    public CreditApplicationResponse applyForCredit(CreditApplyRequest request) {
        Customer customer = findCustomerByIdOrUuid(request.getCustomerId());

        CreditApplication application = new CreditApplication();
        application.setCustomer(customer);
        application.setIncome(request.getIncome());
        application.setEmploymentYears(request.getEmploymentYears());
        application.setLoanAmount(request.getLoanAmount());
        application.setExistingLoans(request.getExistingLoans());
        application.setCreditScore(request.getCreditScore());
        application.setDebtToIncomeRatio(request.getDebtToIncomeRatio());
        application.setApplicationStatus(ApplicationStatus.PENDING);

        CreditApplication savedApplication = creditApplicationRepository.save(application);

        auditLogService.logEvent(
                "CREDIT",
                "CREDIT_APPLICATION",
                savedApplication.getApplicationUuid(),
                AuditAction.CREDIT_APPLICATION_CREATED.name()
        );

        // Publish asynchronous credit underwriting event to RabbitMQ (privacy-preserving)
        com.finvigil.messaging.dto.CreditUnderwritingEvent event = new com.finvigil.messaging.dto.CreditUnderwritingEvent(
                savedApplication.getApplicationUuid(),
                customer.getCustomerUuid(),
                savedApplication.getIncome(),
                savedApplication.getEmploymentYears(),
                savedApplication.getLoanAmount(),
                savedApplication.getExistingLoans(),
                savedApplication.getCreditScore(),
                savedApplication.getDebtToIncomeRatio()
        );
        creditEventPublisher.publishCreditUnderwritingEvent(event);

        return mapToResponse(savedApplication);
    }

    @Transactional(readOnly = true)
    public CreditApplicationResponse getApplicationByIdOrUuid(String idOrUuid) {
        CreditApplication application = findApplicationByIdOrUuid(idOrUuid);
        CreditApplicationResponse response = mapToResponse(application);

        Optional<CreditDecision> decisionOpt = creditDecisionRepository
                .findByApplication_ApplicationUuid(application.getApplicationUuid());
        decisionOpt.ifPresent(decision -> response.setDecision(mapDecisionToResponse(decision)));

        return response;
    }

    @Transactional(readOnly = true)
    public List<CreditApplicationResponse> getApplicationsByCustomerIdOrUuid(String customerIdOrUuid) {
        Customer customer = findCustomerByIdOrUuid(customerIdOrUuid);
        List<CreditApplication> applications = creditApplicationRepository
                .findByCustomer_CustomerUuidOrderByCreatedAtDesc(customer.getCustomerUuid());

        return applications.stream()
                .map(app -> {
                    CreditApplicationResponse response = mapToResponse(app);
                    creditDecisionRepository.findByApplication_ApplicationUuid(app.getApplicationUuid())
                            .ifPresent(d -> response.setDecision(mapDecisionToResponse(d)));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CreditApplication> getLatestApplicationByCustomerId(Long customerId) {
        return creditApplicationRepository.findFirstByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<CreditDecision> getLatestDecisionByCustomerUuid(String customerUuid) {
        return creditDecisionRepository.findFirstByApplication_Customer_CustomerUuidOrderByCreatedAtDesc(customerUuid);
    }

    private Customer findCustomerByIdOrUuid(String customerIdOrUuid) {
        if (customerIdOrUuid.matches("^\\d+$")) {
            Long id = Long.parseLong(customerIdOrUuid);
            return customerRepository.findById(id)
                    .orElseGet(() -> customerRepository.findByCustomerUuid(customerIdOrUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerIdOrUuid)));
        } else {
            return customerRepository.findByCustomerUuid(customerIdOrUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerUuid", customerIdOrUuid));
        }
    }

    private CreditApplication findApplicationByIdOrUuid(String idOrUuid) {
        if (idOrUuid.matches("^\\d+$")) {
            Long id = Long.parseLong(idOrUuid);
            return creditApplicationRepository.findById(id)
                    .orElseGet(() -> creditApplicationRepository.findByApplicationUuid(idOrUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("CreditApplication", "id", idOrUuid)));
        } else {
            return creditApplicationRepository.findByApplicationUuid(idOrUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("CreditApplication", "applicationUuid", idOrUuid));
        }
    }

    public CreditApplicationResponse mapToResponse(CreditApplication app) {
        return new CreditApplicationResponse(
                app.getApplicationUuid(),
                app.getCustomer().getCustomerUuid(),
                app.getIncome(),
                app.getEmploymentYears(),
                app.getLoanAmount(),
                app.getExistingLoans(),
                app.getCreditScore(),
                app.getDebtToIncomeRatio(),
                app.getApplicationStatus(),
                app.getCreatedAt()
        );
    }

    public CreditDecisionResponse mapDecisionToResponse(CreditDecision decision) {
        return new CreditDecisionResponse(
                decision.getDecisionUuid(),
                decision.getApplication().getApplicationUuid(),
                decision.getRiskScore(),
                decision.getRiskLevel(),
                decision.getDecision(),
                decision.getModelVersion(),
                decision.getCreatedAt()
        );
    }
}
