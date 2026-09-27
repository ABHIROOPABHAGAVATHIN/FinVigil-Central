package com.finvigil;

import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/finvigil_db",
        "spring.datasource.username=finvigil_user",
        "spring.datasource.password=finvigil_secure_password",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "app.employee.default-admin.password=TestE2EAdminPass123!"
})
public class AmlPostgresRealIngestionTest {

    @Autowired
    private AmlAlertService amlAlertService;

    @Autowired
    private AmlAlertRepository amlAlertRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Perform real AML alert ingestion into PostgreSQL finvigil_db")
    void testRealPostgresIngestion() {
        Customer customer = customerRepository.findById(1L).orElseGet(() -> {
            Customer newCust = new Customer("Test Ingestion", "real_ingest@finvigil.com", "+1122334455", "hash");
            newCust.setCustomerUuid("cust-ingest-" + UUID.randomUUID());
            return customerRepository.save(newCust);
        });

        String alertUuid = UUID.randomUUID().toString();
        String txnUuid = UUID.randomUUID().toString();

        AmlAlertEvent event = new AmlAlertEvent();
        event.setAlertUuid(alertUuid);
        event.setTransactionUuid(txnUuid);
        event.setCustomerUuid(customer.getCustomerUuid());
        event.setRuleScore(0.61);
        event.setAnomalyScore(0.72);
        event.setHybridScore(0.6538);
        event.setRiskLevel(RiskLevel.HIGH);
        event.setStatus(AlertStatus.OPEN);
        event.setReasons(List.of("LARGE_TRANSACTION_THRESHOLD", "HIGH_RISK_MERCHANT"));
        event.setTimestamp("2026-09-14T20:30:00Z");

        // Ingest into real PostgreSQL database
        AmlAlert savedAlert = amlAlertService.processAlertEvent(event);

        assertNotNull(savedAlert);
        assertNotNull(savedAlert.getId());

        // Read back from repository
        AmlAlert fromDb = amlAlertRepository.findByAlertUuid(alertUuid).orElseThrow();
        assertNotNull(fromDb.getAnomalyScore(), "anomaly_score must not be null");
        assertEquals(0.72, fromDb.getAnomalyScore(), 0.0001);
        assertNotNull(fromDb.getRuleScore(), "rule_score must not be null");
        assertEquals(0.61, fromDb.getRuleScore(), 0.0001);
        assertNotNull(fromDb.getHybridScore(), "hybrid_score must not be null");
        assertEquals(0.6538, fromDb.getHybridScore(), 0.0001);
        assertEquals(RiskLevel.HIGH, fromDb.getRiskLevel());

        System.out.println("SUCCESSFULLY INGESTED REAL AML ALERT INTO POSTGRESQL:");
        System.out.println("Alert UUID: " + fromDb.getAlertUuid());
        System.out.println("DB ID: " + fromDb.getId());
        System.out.println("Anomaly Score: " + fromDb.getAnomalyScore());
        System.out.println("Rule Score: " + fromDb.getRuleScore());
        System.out.println("Hybrid Score: " + fromDb.getHybridScore());
        System.out.println("Risk Level: " + fromDb.getRiskLevel());
    }
}
