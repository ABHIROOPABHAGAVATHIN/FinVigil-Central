package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.LoginRequest;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.common.enums.EmployeeStatus;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.employee.dto.EmployeeAuthResponse;
import com.finvigil.employee.dto.EmployeeCreateRequest;
import com.finvigil.employee.dto.EmployeeLoginRequest;
import com.finvigil.employee.entity.Employee;
import com.finvigil.employee.repository.EmployeeRepository;
import com.finvigil.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private static final String TEST_ADMIN_PASSWORD = "TestAdminPass123!";

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        customerRepository.deleteAll();

        // Seed initial Admin employee for tests
        Employee admin = new Employee(
                "Head Admin",
                "admin@finvigil.internal",
                passwordEncoder.encode(TEST_ADMIN_PASSWORD),
                EmployeeRole.ADMIN,
                EmployeeStatus.ACTIVE
        );
        employeeRepository.save(admin);
    }

    private String getAdminToken() throws Exception {
        EmployeeLoginRequest loginReq = new EmployeeLoginRequest("admin@finvigil.internal", TEST_ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/employee/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        EmployeeAuthResponse authResp = objectMapper.readValue(result.getResponse().getContentAsString(), EmployeeAuthResponse.class);
        return authResp.getToken();
    }

    @Test
    @DisplayName("1. Employee login generates valid JWT with role and userType claims")
    void testEmployeeLogin_ClaimsVerification() throws Exception {
        String adminToken = getAdminToken();
        assertNotNull(adminToken);

        // Verify claims
        assertEquals("admin@finvigil.internal", jwtService.extractUsername(adminToken));
        assertEquals("ADMIN", jwtService.extractRole(adminToken));
        assertEquals("EMPLOYEE", jwtService.extractUserType(adminToken));
        assertNotNull(jwtService.extractEmployeeUuid(adminToken));
    }

    @Test
    @DisplayName("2. Admin can create employees, but unauthorized non-admin receives 403")
    void testAdminCreateEmployee_RbacEnforced() throws Exception {
        String adminToken = getAdminToken();

        // 1. Admin creates AML Analyst
        EmployeeCreateRequest createReq = new EmployeeCreateRequest(
                "Marcus Wright",
                "marcus.aml@finvigil.internal",
                "Marcus@123456",
                EmployeeRole.AML_ANALYST
        );

        mockMvc.perform(post("/api/employee/admin/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Marcus Wright"))
                .andExpect(jsonPath("$.role").value("AML_ANALYST"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.employeeUuid").isNotEmpty());

        // 2. Login as newly created AML Analyst
        EmployeeLoginRequest amlLogin = new EmployeeLoginRequest("marcus.aml@finvigil.internal", "Marcus@123456");
        MvcResult amlResult = mockMvc.perform(post("/api/employee/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amlLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String amlToken = objectMapper.readValue(amlResult.getResponse().getContentAsString(), EmployeeAuthResponse.class).getToken();

        // 3. AML Analyst attempts to create another employee -> Expect 403 Forbidden
        EmployeeCreateRequest unauthorizedReq = new EmployeeCreateRequest(
                "Hacker",
                "hacker@finvigil.internal",
                "Hacker@12345",
                EmployeeRole.ADMIN
        );
        mockMvc.perform(post("/api/employee/admin/employees")
                        .header("Authorization", "Bearer " + amlToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. RBAC demonstration: role-specific endpoints accept authorized roles and reject unauthorized with 403")
    void testRbacDemonstrationEndpoints() throws Exception {
        String adminToken = getAdminToken();

        // Create an AML Analyst and a Credit Analyst
        Employee amlEmp = new Employee("AML Spec", "aml.spec@finvigil.internal", passwordEncoder.encode("AmlPass@123"), EmployeeRole.AML_ANALYST);
        Employee creditEmp = new Employee("Credit Spec", "credit.spec@finvigil.internal", passwordEncoder.encode("CreditPass@123"), EmployeeRole.CREDIT_ANALYST);
        employeeRepository.save(amlEmp);
        employeeRepository.save(creditEmp);

        // Login AML Analyst
        String amlToken = objectMapper.readValue(mockMvc.perform(post("/api/employee/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeLoginRequest("aml.spec@finvigil.internal", "AmlPass@123"))))
                .andReturn().getResponse().getContentAsString(), EmployeeAuthResponse.class).getToken();

        // Login Credit Analyst
        String creditToken = objectMapper.readValue(mockMvc.perform(post("/api/employee/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeLoginRequest("credit.spec@finvigil.internal", "CreditPass@123"))))
                .andReturn().getResponse().getContentAsString(), EmployeeAuthResponse.class).getToken();

        // 1. AML endpoint:
        // AML Analyst -> 200 OK
        mockMvc.perform(get("/api/employee/aml/overview").header("Authorization", "Bearer " + amlToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.scope").value("AML_CONSOLE"));

        // Credit Analyst accessing AML endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/employee/aml/overview").header("Authorization", "Bearer " + creditToken))
                .andExpect(status().isForbidden());

        // Admin accessing AML endpoint -> 200 OK
        mockMvc.perform(get("/api/employee/aml/overview").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 2. Credit endpoint:
        // Credit Analyst -> 200 OK
        mockMvc.perform(get("/api/employee/credit/overview").header("Authorization", "Bearer " + creditToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("CREDIT_CONSOLE"));

        // AML Analyst accessing Credit endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/employee/credit/overview").header("Authorization", "Bearer " + amlToken))
                .andExpect(status().isForbidden());

        // 3. Admin status endpoint:
        // AML Analyst -> 403 Forbidden
        mockMvc.perform(get("/api/employee/admin/status").header("Authorization", "Bearer " + amlToken))
                .andExpect(status().isForbidden());

        // Admin -> 200 OK
        mockMvc.perform(get("/api/employee/admin/status").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ADMIN_CONSOLE"));
    }

    @Test
    @DisplayName("4. Suspended employee is rejected at login with 403")
    void testSuspendedEmployeeLoginRejected() throws Exception {
        Employee suspended = new Employee(
                "Suspended Person",
                "suspended@finvigil.internal",
                passwordEncoder.encode("Pass@12345"),
                EmployeeRole.RISK_ANALYST,
                EmployeeStatus.SUSPENDED
        );
        employeeRepository.save(suspended);

        EmployeeLoginRequest loginReq = new EmployeeLoginRequest("suspended@finvigil.internal", "Pass@12345");

        mockMvc.perform(post("/api/employee/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EMPLOYEE_ACCOUNT_SUSPENDED"));
    }

    @Test
    @DisplayName("5. Customer authentication and authorization still work completely untouched")
    void testCustomerAuthenticationRegression() throws Exception {
        // Register customer
        RegisterRequest registerRequest = new RegisterRequest(
                "John Customer",
                "john.cust@example.com",
                "+1-555-4321",
                "CustomerPass@123"
        );

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.customerUuid").isNotEmpty())
                .andReturn();

        AuthResponse regAuth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        String customerToken = regAuth.getToken();
        String customerUuid = regAuth.getCustomerUuid();

        // Customer login
        LoginRequest loginRequest = new LoginRequest("john.cust@example.com", "CustomerPass@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.cust@example.com"));

        // Customer token accesses customer endpoint by customerUuid -> 200 OK
        mockMvc.perform(get("/api/customers/" + customerUuid)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerUuid").value(customerUuid));


        // Customer token attempts to access internal employee endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/employee/admin/status")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/employee/aml/overview")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
