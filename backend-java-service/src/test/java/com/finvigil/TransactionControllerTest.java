package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.common.enums.TransactionType;
import com.finvigil.credit.repository.CreditApplicationRepository;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.messaging.TransactionEventPublisher;
import com.finvigil.transaction.dto.TransactionCreateRequest;
import com.finvigil.transaction.dto.VelocityStats;
import com.finvigil.transaction.repository.TransactionRepository;
import com.finvigil.transaction.service.RedisVelocityService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.finvigil.aml.repository.AmlAlertRepository amlAlertRepository;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisVelocityService redisVelocityService;

    @MockBean
    private TransactionEventPublisher transactionEventPublisher;

    private String jwtToken;
    private String customerUuid;

    @BeforeEach
    void setUp() {
        amlAlertRepository.deleteAll();
        transactionRepository.deleteAll();
        creditApplicationRepository.deleteAll();
        customerRepository.deleteAll();

        AuthResponse authResponse = authService.register(new RegisterRequest(
                "Txn Test User",
                "txnuser@finvigil.com",
                "+1987654321",
                "TxnSecurePass123"
        ));
        this.jwtToken = authResponse.getToken();
        this.customerUuid = authResponse.getCustomerUuid();

        when(redisVelocityService.recordAndGetVelocity(anyString(), any(BigDecimal.class)))
                .thenReturn(new VelocityStats(1, BigDecimal.valueOf(500.00)));
        when(redisVelocityService.getCurrentVelocity(anyString()))
                .thenReturn(new VelocityStats(1, BigDecimal.valueOf(500.00)));
    }

    @Test
    void testProcessTransactionUnauthorized() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                customerUuid,
                BigDecimal.valueOf(250.00),
                TransactionType.PURCHASE,
                "Walmart",
                "USD",
                OffsetDateTime.now()
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testProcessTransactionSuccess() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                customerUuid,
                BigDecimal.valueOf(250.00),
                TransactionType.PURCHASE,
                "Walmart",
                "USD",
                OffsetDateTime.now()
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionUuid").isNotEmpty())
                .andExpect(jsonPath("$.customerUuid").value(customerUuid))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.transactionType").value("PURCHASE"))
                .andExpect(jsonPath("$.merchant").value("Walmart"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.velocityCount").value(1));
    }

    @Test
    void testGetTransactionsByCustomer() throws Exception {
        // Create a transaction first
        TransactionCreateRequest request = new TransactionCreateRequest(
                customerUuid,
                BigDecimal.valueOf(450.00),
                TransactionType.TRANSFER,
                "TransferWise",
                "USD",
                OffsetDateTime.now()
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Query by customer
        mockMvc.perform(get("/api/transactions/customer/" + customerUuid)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].merchant").value("TransferWise"))
                .andExpect(jsonPath("$[0].amount").value(450.00));
    }
}
