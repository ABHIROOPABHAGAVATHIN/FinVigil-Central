package com.finvigil;

import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.CustomerStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.messaging.AmlEventConsumer;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AmlEndToEndIntegrationTest {

    @Autowired
    private AmlAlertService amlAlertService;

    @Autowired
    private AmlAlertRepository amlAlertRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AmlEventConsumer amlEventConsumer;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private Customer persistedCustomer;

    @BeforeEach
    void setUp() {
        amlAlertRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer("Test Ingestion User", "test.ingest@finvigil.com", "+123456789", "hash");
        customer.setCustomerUuid(UUID.randomUUID().toString());
        customer.setStatus(CustomerStatus.ACTIVE);
        persistedCustomer = customerRepository.save(customer);
    }

    @Test
    @DisplayName("Verify AML alert event persists with non-null anomaly_score, rule_score, hybrid_score, and risk_level")
    @Transactional
    void testAmlAlertEventPersistenceWithAllScores() {
        String alertUuid = UUID.randomUUID().toString();
        String txnUuid = UUID.randomUUID().toString();

        AmlAlertEvent event = new AmlAlertEvent(
                alertUuid,
                txnUuid,
                persistedCustomer.getCustomerUuid(),
                0.61,  // ruleScore
                0.72,  // mlScore / anomalyScore
                0.6538,// hybridScore
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("LARGE_TRANSACTION_THRESHOLD", "HIGH_RISK_MERCHANT"),
                "2026-09-14T20:30:00Z"
        );

        // Ingest via service (identical to consumer execution)
        AmlAlert savedAlert = amlAlertService.processAlertEvent(event);

        assertNotNull(savedAlert);
        assertNotNull(savedAlert.getId(), "Database ID should be generated");

        // Query fresh entity directly from database repository
        AmlAlert fromDb = amlAlertRepository.findByAlertUuid(alertUuid).orElseThrow();

        // 1. Verify anomaly_score is NOT NULL and matches ML score
        assertNotNull(fromDb.getAnomalyScore(), "anomaly_score must NOT be null");
        assertEquals(0.72, fromDb.getAnomalyScore(), 0.0001);
        assertEquals(0.72, fromDb.getMlScore(), 0.0001);

        // 2. Verify rule_score is NOT NULL
        assertNotNull(fromDb.getRuleScore(), "rule_score must NOT be null");
        assertEquals(0.61, fromDb.getRuleScore(), 0.0001);

        // 3. Verify hybrid_score and final_risk_score are NOT NULL
        assertNotNull(fromDb.getHybridScore(), "hybrid_score must NOT be null");
        assertEquals(0.6538, fromDb.getHybridScore(), 0.0001);
        assertEquals(0.6538, fromDb.getFinalRiskScore(), 0.0001);

        // 4. Verify risk_level is HIGH
        assertEquals(RiskLevel.HIGH, fromDb.getRiskLevel());

        // 5. Verify transaction_uuid and customer_id
        assertEquals(txnUuid, fromDb.getTransactionUuid());
        assertEquals(persistedCustomer.getId(), fromDb.getCustomer().getId());
        assertEquals(persistedCustomer.getCustomerUuid(), fromDb.getCustomer().getCustomerUuid());

        // 6. Verify reasons text is NOT NULL
        assertNotNull(fromDb.getReason());
        assertNotNull(fromDb.getReasons());
        assertEquals(AlertStatus.OPEN, fromDb.getStatus());
    }

    @Test
    @DisplayName("Verify AmlEventConsumer handles incoming alert and persists cleanly")
    @Transactional
    void testAmlEventConsumerIngestion() {
        String alertUuid = UUID.randomUUID().toString();
        String txnUuid = UUID.randomUUID().toString();

        AmlAlertEvent event = new AmlAlertEvent();
        event.setAlertUuid(alertUuid);
        event.setTransactionUuid(txnUuid);
        event.setCustomerUuid(persistedCustomer.getCustomerUuid());
        event.setRuleScore(0.35);
        event.setAnomalyScore(0.85); // ML Isolation Forest anomaly score
        event.setHybridScore(0.65);
        event.setRiskLevel(RiskLevel.HIGH);
        event.setStatus(AlertStatus.OPEN);
        event.setReasons(List.of("STRUCTURING_DETECTION"));

        // Call consumer method directly
        amlEventConsumer.consumeAmlAlert(event);

        AmlAlert fromDb = amlAlertRepository.findByAlertUuid(alertUuid).orElseThrow();
        assertNotNull(fromDb.getAnomalyScore(), "anomaly_score must be non-null");
        assertEquals(0.85, fromDb.getAnomalyScore(), 0.0001);
        assertEquals(0.35, fromDb.getRuleScore(), 0.0001);
        assertEquals(0.65, fromDb.getHybridScore(), 0.0001);
        assertEquals(RiskLevel.HIGH, fromDb.getRiskLevel());
    }
}
