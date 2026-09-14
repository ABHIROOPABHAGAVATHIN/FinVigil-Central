package com.finvigil.customer.service;

import com.finvigil.common.enums.AuditAction;
import com.finvigil.customer.dto.CustomerCreateRequest;
import com.finvigil.customer.dto.CustomerProfileResponse;
import com.finvigil.customer.dto.CustomerResponse;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.exception.AppException;
import com.finvigil.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final com.finvigil.credit.repository.CreditApplicationRepository creditApplicationRepository;
    private final com.finvigil.credit.repository.CreditDecisionRepository creditDecisionRepository;
    private final com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository;
    private final com.finvigil.transaction.repository.TransactionRepository transactionRepository;
    private final com.finvigil.transaction.service.RedisVelocityService redisVelocityService;
    private final int velocityLimit;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService,
                           com.finvigil.credit.repository.CreditApplicationRepository creditApplicationRepository,
                           com.finvigil.credit.repository.CreditDecisionRepository creditDecisionRepository,
                           com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository) {
        this(customerRepository, passwordEncoder, auditLogService, creditApplicationRepository, creditDecisionRepository, amlAlertRepository, null, null, 5);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService,
                           com.finvigil.credit.repository.CreditApplicationRepository creditApplicationRepository,
                           com.finvigil.credit.repository.CreditDecisionRepository creditDecisionRepository,
                           com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository,
                           com.finvigil.transaction.repository.TransactionRepository transactionRepository,
                           com.finvigil.transaction.service.RedisVelocityService redisVelocityService,
                           @org.springframework.beans.factory.annotation.Value("${app.aml.velocity-limit:5}") int velocityLimit) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditDecisionRepository = creditDecisionRepository;
        this.amlAlertRepository = amlAlertRepository;
        this.transactionRepository = transactionRepository;
        this.redisVelocityService = redisVelocityService;
        this.velocityLimit = velocityLimit;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new AppException("Email already registered: " + request.getEmail(),
                    HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
        }

        Customer customer = new Customer();
        customer.setName(request.getName().trim());
        customer.setEmail(request.getEmail().toLowerCase().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Customer saved = customerRepository.save(customer);

        auditLogService.logEvent(
                "CUSTOMER",
                "CUSTOMER",
                saved.getCustomerUuid(),
                AuditAction.CUSTOMER_REGISTERED.name()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntityByUuid(String customerUuid) {
        return customerRepository.findByCustomerUuid(customerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerUuid", customerUuid));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByIdOrUuid(String idOrUuid) {
        Customer customer = findEntityByIdOrUuid(idOrUuid);
        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(String idOrUuid) {
        Customer customer = findEntityByIdOrUuid(idOrUuid);
        CustomerResponse customerResponse = mapToResponse(customer);

        // 1. Credit Information
        Map<String, Object> creditInfo = new HashMap<>();
        Double creditRiskScore = null;
        var latestAppOpt = creditApplicationRepository.findFirstByCustomer_CustomerUuidOrderByCreatedAtDesc(customer.getCustomerUuid());
        if (latestAppOpt.isPresent()) {
            var app = latestAppOpt.get();
            creditInfo.put("hasApplication", true);
            creditInfo.put("applicationUuid", app.getApplicationUuid());
            creditInfo.put("applicationStatus", app.getApplicationStatus().name());
            creditInfo.put("loanAmount", app.getLoanAmount());
            creditInfo.put("creditScore", app.getCreditScore());

            var decisionOpt = creditDecisionRepository.findByApplication_ApplicationUuid(app.getApplicationUuid());
            if (decisionOpt.isPresent()) {
                var decision = decisionOpt.get();
                creditInfo.put("latestRiskScore", decision.getRiskScore());
                creditInfo.put("riskLevel", decision.getRiskLevel().name());
                creditInfo.put("decision", decision.getDecision().name());
                if (decision.getRiskScore() != null) {
                    creditRiskScore = decision.getRiskScore().doubleValue();
                }
            }
        } else {
            creditInfo.put("hasApplication", false);
            creditInfo.put("status", "NO_APPLICATION");
        }

        // 2. Transactions & Real-time Redis Velocity
        List<Map<String, Object>> txnList = new ArrayList<>();
        var velocityStats = (redisVelocityService != null)
                ? redisVelocityService.getCurrentVelocity(customer.getCustomerUuid())
                : new com.finvigil.transaction.dto.VelocityStats(0, java.math.BigDecimal.ZERO);

        if (transactionRepository != null) {
            var txns = transactionRepository.findByCustomer_CustomerUuidOrderByTransactionTimestampDesc(customer.getCustomerUuid());
            for (var txn : txns) {
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("transactionUuid", txn.getTransactionUuid());
                tMap.put("amount", txn.getAmount());
                tMap.put("transactionType", txn.getTransactionType() != null ? txn.getTransactionType().name() : null);
                tMap.put("merchant", txn.getMerchant());
                tMap.put("status", txn.getStatus() != null ? txn.getStatus().name() : null);
                tMap.put("timestamp", txn.getTransactionTimestamp());
                txnList.add(tMap);
            }
        }

        // 3. AML Alert Information
        Map<String, Object> amlInfo = new HashMap<>();
        long openAlerts = amlAlertRepository.countByCustomer_CustomerUuidAndStatus(customer.getCustomerUuid(), com.finvigil.common.enums.AlertStatus.OPEN);
        var latestAlertOpt = amlAlertRepository.findFirstByCustomer_CustomerUuidOrderByCreatedAtDesc(customer.getCustomerUuid());
        
        Double amlScore = 0.0;
        com.finvigil.common.enums.RiskLevel latestAmlRiskLevel = com.finvigil.common.enums.RiskLevel.LOW;
        if (latestAlertOpt.isPresent()) {
            var alert = latestAlertOpt.get();
            latestAmlRiskLevel = alert.getRiskLevel();
            amlInfo.put("riskLevel", alert.getRiskLevel().name());
            amlInfo.put("openAlerts", openAlerts);
            amlInfo.put("latestAlertUuid", alert.getAlertUuid());
            amlInfo.put("latestHybridScore", alert.getHybridScore());
            amlInfo.put("anomalyScore", alert.getAnomalyScore());
            amlInfo.put("ruleScore", alert.getRuleScore());
            amlInfo.put("latestStatus", alert.getStatus().name());
            if (alert.getStatus() == com.finvigil.common.enums.AlertStatus.OPEN && alert.getHybridScore() != null) {
                amlScore = alert.getHybridScore();
            }
        } else {
            amlInfo.put("riskLevel", "LOW");
            amlInfo.put("openAlerts", openAlerts);
        }

        // 4. Deterministic Customer-Level Risk Aggregation
        int effectiveLimit = velocityLimit > 0 ? velocityLimit : 5;
        double velocityScore = Math.min(1.0, (double) velocityStats.getCount() / effectiveLimit);

        double compositeScore;
        if (creditRiskScore != null) {
            compositeScore = (0.50 * amlScore) + (0.30 * creditRiskScore) + (0.20 * velocityScore);
        } else {
            compositeScore = (0.70 * amlScore) + (0.30 * velocityScore);
        }
        compositeScore = Math.round(compositeScore * 10000.0) / 10000.0;

        com.finvigil.common.enums.RiskLevel overallRiskLevel;
        if (compositeScore >= 0.60 || (openAlerts > 0 && latestAmlRiskLevel == com.finvigil.common.enums.RiskLevel.HIGH)) {
            overallRiskLevel = com.finvigil.common.enums.RiskLevel.HIGH;
        } else if (compositeScore >= 0.30 || (openAlerts > 0 && latestAmlRiskLevel == com.finvigil.common.enums.RiskLevel.MEDIUM)) {
            overallRiskLevel = com.finvigil.common.enums.RiskLevel.MEDIUM;
        } else {
            overallRiskLevel = com.finvigil.common.enums.RiskLevel.LOW;
        }

        com.finvigil.common.enums.CustomerStatus accountStanding;
        if (openAlerts > 0 && latestAmlRiskLevel == com.finvigil.common.enums.RiskLevel.HIGH) {
            accountStanding = com.finvigil.common.enums.CustomerStatus.UNDER_INVESTIGATION;
        } else {
            accountStanding = customer.getStatus() != null ? customer.getStatus() : com.finvigil.common.enums.CustomerStatus.ACTIVE;
        }

        com.finvigil.common.enums.CreditDecisionType recommendedAction;
        if (overallRiskLevel == com.finvigil.common.enums.RiskLevel.HIGH) {
            recommendedAction = com.finvigil.common.enums.CreditDecisionType.REVIEW;
        } else if (overallRiskLevel == com.finvigil.common.enums.RiskLevel.MEDIUM) {
            recommendedAction = com.finvigil.common.enums.CreditDecisionType.REVIEW;
        } else {
            recommendedAction = com.finvigil.common.enums.CreditDecisionType.APPROVE;
        }

        List<String> contributingFactors = new ArrayList<>();
        if (openAlerts > 0 && latestAmlRiskLevel == com.finvigil.common.enums.RiskLevel.HIGH) {
            contributingFactors.add("OPEN_HIGH_RISK_AML_ALERT");
        }
        if (openAlerts > 0 && latestAmlRiskLevel == com.finvigil.common.enums.RiskLevel.MEDIUM) {
            contributingFactors.add("OPEN_MEDIUM_RISK_AML_ALERT");
        }
        if (velocityStats.getCount() >= effectiveLimit) {
            contributingFactors.add("HIGH_TRANSACTION_VELOCITY");
        }

        Map<String, Object> riskAggregation = new HashMap<>();
        riskAggregation.put("overallRiskLevel", overallRiskLevel.name());
        riskAggregation.put("compositeRiskScore", compositeScore);
        riskAggregation.put("accountStanding", accountStanding.name());
        riskAggregation.put("recommendedAction", recommendedAction.name());
        riskAggregation.put("contributingFactors", contributingFactors);
        riskAggregation.put("currentVelocityCount", velocityStats.getCount());
        riskAggregation.put("currentVelocityAmount", velocityStats.getTotalAmount());

        return new CustomerProfileResponse(
                customerResponse,
                creditInfo,
                txnList,
                amlInfo,
                riskAggregation
        );
    }

    private Customer findEntityByIdOrUuid(String idOrUuid) {
        if (idOrUuid.matches("^\\d+$")) {
            Long id = Long.parseLong(idOrUuid);
            return customerRepository.findById(id)
                    .orElseGet(() -> customerRepository.findByCustomerUuid(idOrUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", idOrUuid)));
        } else {
            return customerRepository.findByCustomerUuid(idOrUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerUuid", idOrUuid));
        }
    }

    public CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerUuid(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}
