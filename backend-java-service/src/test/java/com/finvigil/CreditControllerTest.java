package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.credit.dto.CreditApplyRequest;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.customer.repository.CustomerRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private String jwtToken;
    private String customerUuid;

    @BeforeEach
    void setUp() {
        amlAlertRepository.deleteAll();
        creditApplicationRepository.deleteAll();
        customerRepository.deleteAll();

        AuthResponse authResponse = authService.register(new RegisterRequest(
                "John Borrower",
                "borrower@finvigil.com",
                "+1987654321",
                "SecurePass123"
        ));
        this.jwtToken = authResponse.getToken();
        this.customerUuid = authResponse.getCustomerUuid();
    }

    @Test
    void testApplyCreditValid() throws Exception {
        CreditApplyRequest request = new CreditApplyRequest(
                customerUuid,
                new BigDecimal("75000.00"),
                5,
                new BigDecimal("250000.00"),
                1,
                740,
                new BigDecimal("0.28")
        );

        mockMvc.perform(post("/api/credit/apply")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.applicationUuid").isNotEmpty())
                .andExpect(jsonPath("$.customerUuid").value(customerUuid))
                .andExpect(jsonPath("$.income").value(75000.00))
                .andExpect(jsonPath("$.loanAmount").value(250000.00))
                .andExpect(jsonPath("$.creditScore").value(740))
                .andExpect(jsonPath("$.applicationStatus").value("PENDING"));
    }

    @Test
    void testApplyCreditNegativeIncomeRejected() throws Exception {
        CreditApplyRequest request = new CreditApplyRequest(
                customerUuid,
                new BigDecimal("-5000.00"),
                5,
                new BigDecimal("100000.00"),
                0,
                700,
                new BigDecimal("0.30")
        );

        mockMvc.perform(post("/api/credit/apply")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testApplyCreditInvalidCreditScoreRejected() throws Exception {
        CreditApplyRequest request = new CreditApplyRequest(
                customerUuid,
                new BigDecimal("60000.00"),
                3,
                new BigDecimal("100000.00"),
                0,
                250, // Invalid credit score (< 300)
                new BigDecimal("0.30")
        );

        mockMvc.perform(post("/api/credit/apply")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testApplyCreditUnknownCustomerRejected() throws Exception {
        CreditApplyRequest request = new CreditApplyRequest(
                "unknown-uuid-00000000",
                new BigDecimal("60000.00"),
                3,
                new BigDecimal("100000.00"),
                0,
                700,
                new BigDecimal("0.30")
        );

        mockMvc.perform(post("/api/credit/apply")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void testGetCreditApplicationAndCustomerList() throws Exception {
        CreditApplyRequest request = new CreditApplyRequest(
                customerUuid,
                new BigDecimal("80000.00"),
                6,
                new BigDecimal("300000.00"),
                2,
                720,
                new BigDecimal("0.35")
        );

        String responseStr = mockMvc.perform(post("/api/credit/apply")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String applicationUuid = objectMapper.readTree(responseStr).get("applicationUuid").asText();

        // Query by Application UUID
        mockMvc.perform(get("/api/credit/application/" + applicationUuid)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationUuid").value(applicationUuid))
                .andExpect(jsonPath("$.creditScore").value(720));

        // Query by Customer UUID
        mockMvc.perform(get("/api/credit/customer/" + customerUuid)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].applicationUuid").value(applicationUuid));
    }
}
