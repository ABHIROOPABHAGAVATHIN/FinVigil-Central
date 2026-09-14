package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.aml.dto.AlertStatusUpdateRequest;
import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.repository.AmlAlertRepository;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AmlAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AmlAlertRepository amlAlertRepository;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private String jwtToken;
    private Customer testCustomer;
    private AmlAlert testAlert;

    @BeforeEach
    void setUp() {
        amlAlertRepository.deleteAll();
        transactionRepository.deleteAll();
        creditApplicationRepository.deleteAll();
        customerRepository.deleteAll();

        AuthResponse authResponse = authService.register(new RegisterRequest(
                "Compliance Lead",
                "compliancelead@finvigil.com",
                "+1999888777",
                "CompliancePass123"
        ));
        this.jwtToken = authResponse.getToken();

        testCustomer = customerRepository.findByCustomerUuid(authResponse.getCustomerUuid()).orElseThrow();

        AmlAlert alert = new AmlAlert(
                testCustomer,
                "txn-sample-001",
                0.40,
                0.75,
                0.61,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("LARGE_TRANSACTION_THRESHOLD", "HIGH_RISK_MERCHANT")
        );
        alert.setAlertUuid("alert-test-uuid-99");
        this.testAlert = amlAlertRepository.save(alert);
    }

    @Test
    void testGetAllAlertsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/aml/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllAlertsAuthorized() throws Exception {
        mockMvc.perform(get("/api/aml/alerts")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].alertUuid").value("alert-test-uuid-99"))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].hybridScore").value(0.61));
    }

    @Test
    void testGetAlertByUuid() throws Exception {
        mockMvc.perform(get("/api/aml/alerts/alert-test-uuid-99")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertUuid").value("alert-test-uuid-99"))
                .andExpect(jsonPath("$.customerUuid").value(testCustomer.getCustomerUuid()))
                .andExpect(jsonPath("$.transactionUuid").value("txn-sample-001"))
                .andExpect(jsonPath("$.ruleScore").value(0.40))
                .andExpect(jsonPath("$.anomalyScore").value(0.75))
                .andExpect(jsonPath("$.hybridScore").value(0.61))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.reasons").isArray());
    }

    @Test
    void testGetAlertsByCustomer() throws Exception {
        mockMvc.perform(get("/api/aml/alerts/customer/" + testCustomer.getCustomerUuid())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].alertUuid").value("alert-test-uuid-99"));
    }

    @Test
    void testUpdateAlertStatus() throws Exception {
        AlertStatusUpdateRequest updateRequest = new AlertStatusUpdateRequest(
                AlertStatus.UNDER_REVIEW,
                "Case escalated to Senior AML Officer"
        );

        mockMvc.perform(patch("/api/aml/alerts/alert-test-uuid-99/status")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.resolutionNotes").value("Case escalated to Senior AML Officer"));
    }
}
