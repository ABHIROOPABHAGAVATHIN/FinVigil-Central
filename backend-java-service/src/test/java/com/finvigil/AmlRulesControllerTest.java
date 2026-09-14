package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.aml.dto.RuleEvaluationRequest;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.common.enums.TransactionType;
import com.finvigil.credit.repository.CreditApplicationRepository;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AmlRulesControllerTest {

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
    private com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        amlAlertRepository.deleteAll();
        transactionRepository.deleteAll();
        creditApplicationRepository.deleteAll();
        customerRepository.deleteAll();

        AuthResponse authResponse = authService.register(new RegisterRequest(
                "AML Compliance Officer",
                "amlcompliance@finvigil.com",
                "+1987111222",
                "AmlSecurePass123"
        ));
        this.jwtToken = authResponse.getToken();
    }

    @Test
    void testGetActiveRulesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/aml/rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetActiveRulesAuthorized() throws Exception {
        mockMvc.perform(get("/api/aml/rules")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ruleName").isNotEmpty())
                .andExpect(jsonPath("$[0].ruleType").isNotEmpty());
    }

    @Test
    void testEvaluateTransactionEndpoint() throws Exception {
        RuleEvaluationRequest request = new RuleEvaluationRequest(
                "test-txn-123",
                "test-cust-456",
                BigDecimal.valueOf(85000.0),
                TransactionType.TRANSFER,
                "Offshore Crypto Services",
                "INR",
                OffsetDateTime.now(),
                6,
                BigDecimal.valueOf(150000.0)
        );

        mockMvc.perform(post("/api/aml/rules/evaluate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionUuid").value("test-txn-123"))
                .andExpect(jsonPath("$.customerUuid").value("test-cust-456"))
                .andExpect(jsonPath("$.ruleScore").isNotEmpty())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.alertTriggered").value(true))
                .andExpect(jsonPath("$.triggeredRules").isArray())
                .andExpect(jsonPath("$.violations").isArray());
    }
}
