package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.auth.service.AuthService;
import com.finvigil.customer.dto.CustomerCreateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private com.finvigil.credit.repository.CreditApplicationRepository creditApplicationRepository;

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
                "Admin User",
                "admin@finvigil.com",
                "+1234567890",
                "AdminSecure123"
        ));
        this.jwtToken = authResponse.getToken();
        this.customerUuid = authResponse.getCustomerUuid();
    }

    @Test
    void testGetCustomerWithoutTokenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerUuid))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetCustomerWithValidToken() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerUuid)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerUuid").value(customerUuid))
                .andExpect(jsonPath("$.email").value("admin@finvigil.com"))
                .andExpect(jsonPath("$.name").value("Admin User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetCustomerProfileWithValidToken() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerUuid + "/profile")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerUuid").value(customerUuid))
                .andExpect(jsonPath("$.credit").exists())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.aml").exists());
    }

    @Test
    void testCreateCustomerAuthenticated() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "New Client",
                "client@example.com",
                "+1555123456",
                "Password987"
        );

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerUuid").isNotEmpty())
                .andExpect(jsonPath("$.email").value("client@example.com"))
                .andExpect(jsonPath("$.name").value("New Client"));
    }
}
