package com.finvigil;

import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.LoginRequest;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.common.enums.TransactionType;
import com.finvigil.credit.dto.CreditApplyRequest;
import com.finvigil.credit.entity.CreditDecision;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.credit.repository.CreditDecisionRepository;
import com.finvigil.credit.service.CreditService;
import com.finvigil.customer.dto.CustomerProfileResponse;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.CustomerService;
import com.finvigil.transaction.dto.TransactionCreateRequest;
import com.finvigil.transaction.dto.TransactionResponse;
import com.finvigil.transaction.repository.TransactionRepository;
import com.finvigil.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/finvigil_db",
        "spring.datasource.username=finvigil_user",
        "spring.datasource.password=finvigil_secure_password",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "app.employee.default-admin.password=TestE2EAdminPass123!"
})
public class FinVigilFullSystemE2ETest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditService creditService;

    @Autowired
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    private CreditDecisionRepository creditDecisionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AmlAlertRepository amlAlertRepository;

    @Test
    @DisplayName("Phase 15 Full-System E2E: Register -> Login -> Credit Underwriting -> Transaction -> Velocity -> AML Alert -> PostgreSQL -> Unified Customer Profile")
    void testCompleteFinVigilE2EPipeline() throws Exception {
        System.out.println("================================================================================");
        System.out.println("STARTING PHASE 15 FULL-SYSTEM END-TO-END PIPELINE INTEGRATION TEST");
        System.out.println("================================================================================");

        // 1. FRESH CUSTOMER REGISTRATION
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String testEmail = "e2e_phase15_" + uniqueSuffix + "@finvigil.com";
        String testPassword = "SecurePassword123!";

        RegisterRequest registerRequest = new RegisterRequest(
                "Phase15 E2E Customer",
                testEmail,
                "+1" + (1000000000L + (long)(Math.random() * 8999999999L)),
                testPassword
        );

        AuthResponse authResponse = authService.register(registerRequest);
        assertNotNull(authResponse, "Auth response must not be null");
        assertNotNull(authResponse.getToken(), "JWT token must be generated");
        assertNotNull(authResponse.getCustomerUuid(), "Customer UUID must be generated");

        String customerUuid = authResponse.getCustomerUuid();
        System.out.println("[Step 1] Customer registered successfully. UUID: " + customerUuid + ", Email: " + testEmail);

        // 2. JWT LOGIN VERIFICATION
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);
        AuthResponse loginResponse = authService.login(loginRequest);
        assertNotNull(loginResponse.getToken(), "Login JWT token must be valid");
        assertEquals(customerUuid, loginResponse.getCustomerUuid());
        System.out.println("[Step 2] JWT login verified successfully.");

        // 3. CREDIT APPLICATION SUBMISSION
        CreditApplyRequest creditApplyRequest = new CreditApplyRequest(
                customerUuid,
                BigDecimal.valueOf(125000.00), // High income
                7,                            // 7 years employment
                BigDecimal.valueOf(25000.00),  // Reasonable loan amount
                0,                            // 0 existing loans
                780,                          // Excellent credit score
                BigDecimal.valueOf(0.15)       // Low DTI
        );

        var creditAppResponse = creditService.applyForCredit(creditApplyRequest);
        assertNotNull(creditAppResponse);
        assertNotNull(creditAppResponse.getApplicationUuid());
        System.out.println("[Step 3] Credit application submitted. App UUID: " + creditAppResponse.getApplicationUuid());

        // 4. AWAIT PYTHON XGBOOST CREDIT DECISION VIA RABBITMQ
        System.out.println("[Step 4] Awaiting Python XGBoost underwriting decision from RabbitMQ...");
        Optional<CreditDecision> decisionOpt = Optional.empty();
        for (int i = 0; i < 20; i++) {
            decisionOpt = creditDecisionRepository.findByApplication_ApplicationUuid(creditAppResponse.getApplicationUuid());
            if (decisionOpt.isPresent()) {
                break;
            }
            Thread.sleep(500);
        }

        assertTrue(decisionOpt.isPresent(), "Credit decision must be persisted via RabbitMQ consumer");
        CreditDecision decision = decisionOpt.get();
        assertNotNull(decision.getRiskScore(), "Credit riskScore must be present");
        assertTrue(decision.getRiskScore().doubleValue() >= 0.0 && decision.getRiskScore().doubleValue() <= 1.0,
                "Credit riskScore must be between 0.0 and 1.0");
        System.out.println("[Step 4] Credit decision received: Decision=" + decision.getDecision() +
                ", RiskLevel=" + decision.getRiskLevel() + ", RiskScore=" + decision.getRiskScore());

        // 5. DETERMINISTIC SUSPICIOUS TRANSACTION CREATION
        // Amount = 75000.00 (>= 50000 large-txn threshold), Merchant = "Crypto Casino Royale" (high risk)
        TransactionCreateRequest txnRequest = new TransactionCreateRequest(
                customerUuid,
                BigDecimal.valueOf(75000.00),
                TransactionType.TRANSFER,
                "Crypto Casino Royale",
                "INR",
                OffsetDateTime.now()
        );

        TransactionResponse txnResponse = transactionService.processTransaction(txnRequest);
        assertNotNull(txnResponse);
        assertNotNull(txnResponse.getTransactionUuid());
        System.out.println("[Step 5] Suspicious transaction recorded. Txn UUID: " + txnResponse.getTransactionUuid() +
                ", Amount: " + txnResponse.getAmount() + ", VelocityCount: " + txnResponse.getVelocityCount());

        // 6. AWAIT PYTHON AML WORKER INFERENCE & JAVA ALERT INGESTION INTO POSTGRESQL
        System.out.println("[Step 6] Awaiting Python AML rules + Isolation Forest evaluation and Java alert ingestion...");
        Optional<AmlAlert> alertOpt = Optional.empty();
        for (int i = 0; i < 30; i++) {
            alertOpt = amlAlertRepository.findFirstByCustomer_CustomerUuidOrderByCreatedAtDesc(customerUuid);
            if (alertOpt.isPresent()) {
                break;
            }
            Thread.sleep(500);
        }

        assertTrue(alertOpt.isPresent(), "AML alert must be generated and persisted into PostgreSQL aml_alerts");
        AmlAlert alert = alertOpt.get();

        // 7. CRITICAL E2E ASSERTIONS
        System.out.println("[Step 7] Verifying AML scores and PostgreSQL constraints:");
        System.out.println("   - Alert UUID: " + alert.getAlertUuid());
        System.out.println("   - Anomaly Score: " + alert.getAnomalyScore());
        System.out.println("   - Rule Score: " + alert.getRuleScore());
        System.out.println("   - Hybrid Score: " + alert.getHybridScore());
        System.out.println("   - Risk Level: " + alert.getRiskLevel());

        // Assertion 1 & 2: anomalyScore is present and within [0.0, 1.0]
        assertNotNull(alert.getAnomalyScore(), "anomaly_score must NOT be null in PostgreSQL");
        assertTrue(alert.getAnomalyScore() >= 0.0 && alert.getAnomalyScore() <= 1.0,
                "anomalyScore must be within [0.0, 1.0], observed: " + alert.getAnomalyScore());

        // Assertion 3: ruleScore reflects deliberately triggered rules (0.61)
        assertNotNull(alert.getRuleScore(), "rule_score must NOT be null");
        assertEquals(0.61, alert.getRuleScore(), 0.01,
                "Rule score must reflect Rule 1 (0.35) and Rule 4 (0.40)");

        // Assertion 4: hybridScore satisfies 0.40 * ruleScore + 0.60 * anomalyScore
        assertNotNull(alert.getHybridScore(), "hybrid_score must NOT be null");
        double expectedHybrid = (0.40 * alert.getRuleScore()) + (0.60 * alert.getAnomalyScore());
        assertEquals(expectedHybrid, alert.getHybridScore(), 0.005,
                "hybridScore must strictly satisfy (0.40 * ruleScore) + (0.60 * anomalyScore)");

        // Assertion 5 & 6: risk level is HIGH and status is OPEN
        assertEquals(RiskLevel.HIGH, alert.getRiskLevel(), "AML risk level must be HIGH");
        assertEquals(AlertStatus.OPEN, alert.getStatus(), "AML alert status must be OPEN");

        // 8. UNIFIED CUSTOMER PROFILE VERIFICATION
        System.out.println("[Step 8] Verifying Unified Customer Profile endpoint...");
        CustomerProfileResponse profile = customerService.getCustomerProfile(customerUuid);
        assertNotNull(profile);
        assertNotNull(profile.getCustomer());
        assertEquals(customerUuid, profile.getCustomer().getCustomerUuid());
        assertEquals(testEmail, profile.getCustomer().getEmail());

        // Verify Credit profile block
        Map<String, Object> creditBlock = profile.getCredit();
        assertNotNull(creditBlock);
        assertEquals(true, creditBlock.get("hasApplication"));
        assertNotNull(creditBlock.get("applicationUuid"));
        assertNotNull(creditBlock.get("latestRiskScore"));

        // Verify Transactions profile block
        assertNotNull(profile.getTransactions());
        assertFalse(profile.getTransactions().isEmpty(), "Transaction history must not be empty");
        Map<String, Object> latestTxnMap = profile.getTransactions().get(0);
        assertEquals(txnResponse.getTransactionUuid(), latestTxnMap.get("transactionUuid"));

        // Verify AML profile block
        Map<String, Object> amlBlock = profile.getAml();
        assertNotNull(amlBlock);
        assertEquals(1L, ((Number) amlBlock.get("openAlerts")).longValue());
        assertEquals("HIGH", amlBlock.get("riskLevel"));
        assertNotNull(amlBlock.get("latestAlertUuid"));
        assertNotNull(amlBlock.get("anomalyScore"));

        // Verify Risk Aggregation block
        Map<String, Object> riskAgg = profile.getRiskAggregation();
        assertNotNull(riskAgg, "Risk aggregation block must be populated");
        assertEquals("HIGH", riskAgg.get("overallRiskLevel"));
        assertEquals("UNDER_INVESTIGATION", riskAgg.get("accountStanding"));
        assertNotNull(riskAgg.get("compositeRiskScore"));
        assertNotNull(riskAgg.get("recommendedAction"));
        assertNotNull(riskAgg.get("contributingFactors"));

        System.out.println("[Step 8] Unified Customer Profile fully verified:");
        System.out.println("   - Customer: " + profile.getCustomer().getName() + " (" + profile.getCustomer().getCustomerUuid() + ")");
        System.out.println("   - Credit Decision: " + creditBlock.get("decision") + " (RiskScore: " + creditBlock.get("latestRiskScore") + ")");
        System.out.println("   - Transactions Count: " + profile.getTransactions().size());
        System.out.println("   - AML Open Alerts: " + amlBlock.get("openAlerts") + ", Level: " + amlBlock.get("riskLevel"));
        System.out.println("   - Overall Aggregated Risk Level: " + riskAgg.get("overallRiskLevel") + ", CompositeScore: " + riskAgg.get("compositeRiskScore"));
        System.out.println("   - Account Standing: " + riskAgg.get("accountStanding"));
        System.out.println("================================================================================");
        System.out.println("PHASE 15 FULL-SYSTEM E2E INTEGRATION TEST COMPLETED SUCCESSFULLY!");
        System.out.println("================================================================================");
    }
}
