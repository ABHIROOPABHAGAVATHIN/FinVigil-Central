package com.finvigil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvigil.common.enums.CustomerStatus;
import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.common.enums.EmployeeStatus;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerDirectoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    private String adminToken;
    private String riskAnalystToken;
    private String amlAnalystToken;
    private String creditAnalystToken;
    private String viewerToken;
    private String customerToken;

    private Customer alice;
    private Customer bob;
    private Customer charlie;
    private Customer diana;
    private Customer edward;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        employeeRepository.deleteAll();

        // Seed 5 test customers with diverse attributes
        alice = createCustomer("Alice Smith", "alice.smith@example.com", "+1-555-100-0001", CustomerStatus.ACTIVE);
        bob = createCustomer("Bob Jones", "bob.jones@example.com", "+1-555-200-0002", CustomerStatus.ACTIVE);
        charlie = createCustomer("Charlie Brown", "charlie.brown@example.com", "+1-555-300-0003", CustomerStatus.SUSPENDED);
        diana = createCustomer("Diana Prince", "diana.prince@example.com", "+1-555-400-0004", CustomerStatus.ACTIVE);
        edward = createCustomer("Edward Norton", "edward.norton@example.com", "+1-555-500-0005", CustomerStatus.ACTIVE);

        // Seed employees for each role
        adminToken = createEmployeeAndGenerateToken("Admin User", "admin@finvigil.internal", EmployeeRole.ADMIN);
        riskAnalystToken = createEmployeeAndGenerateToken("Risk User", "risk@finvigil.internal", EmployeeRole.RISK_ANALYST);
        amlAnalystToken = createEmployeeAndGenerateToken("AML User", "aml@finvigil.internal", EmployeeRole.AML_ANALYST);
        creditAnalystToken = createEmployeeAndGenerateToken("Credit User", "credit@finvigil.internal", EmployeeRole.CREDIT_ANALYST);
        viewerToken = createEmployeeAndGenerateToken("Viewer User", "viewer@finvigil.internal", EmployeeRole.VIEWER);

        // Customer token for cross-domain RBAC rejection
        customerToken = jwtService.generateToken(alice.getEmail(), alice.getCustomerUuid());
    }

    private Customer createCustomer(String name, String email, String phone, CustomerStatus status) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setPasswordHash(passwordEncoder.encode("CustPass123!"));
        c.setStatus(status);
        c.setCustomerUuid("cust-" + UUID.randomUUID());
        return customerRepository.save(c);
    }

    private String createEmployeeAndGenerateToken(String name, String email, EmployeeRole role) {
        String uuid = "emp-" + UUID.randomUUID();
        Employee emp = new Employee(name, email, passwordEncoder.encode("EmpPass123!"), role, EmployeeStatus.ACTIVE);
        emp.setEmployeeUuid(uuid);
        employeeRepository.save(emp);
        return jwtService.generateEmployeeToken(email, uuid, role);
    }

    // =========================================================================
    // 1. Role-Based Access Control Tests
    // =========================================================================

    @Test
    @DisplayName("1. All employee roles can access the customer directory (/api/employee/customers)")
    void testEmployeeRolesCanAccessDirectory() throws Exception {
        // ADMIN
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));

        // RISK_ANALYST
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + riskAnalystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));

        // AML_ANALYST
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + amlAnalystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));

        // CREDIT_ANALYST
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));

        // VIEWER
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("2. All employee roles can access /api/customers directory")
    void testEmployeeRolesCanAccessCustomerControllerDirectory() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("3. Customer JWT is rejected from customer directory with 403 Forbidden")
    void testCustomerJwtRejectedFromDirectory() throws Exception {
        // Calling /api/employee/customers with customer token
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Calling /api/customers with customer token
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request is rejected with 403 Forbidden")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/employee/customers"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. Search & Filtering Tests
    // =========================================================================

    @Test
    @DisplayName("5. Search by name matches case-insensitively")
    void testSearchByName() throws Exception {
        // Lowercase query
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "alice")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Smith")))
                .andExpect(jsonPath("$.content[0].email", is("alice.smith@example.com")));

        // Uppercase query
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "ALICE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Smith")));
    }

    @Test
    @DisplayName("6. Search by email substring")
    void testSearchByEmail() throws Exception {
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "bob.jones")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Jones")));
    }

    @Test
    @DisplayName("7. Search by phone number")
    void testSearchByPhone() throws Exception {
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "300-0003")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Charlie Brown")));
    }

    @Test
    @DisplayName("8. Search by customerUuid substring")
    void testSearchByUuid() throws Exception {
        String uuidSub = diana.getCustomerUuid().substring(0, 10);
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", uuidSub)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Diana Prince")));
    }

    @Test
    @DisplayName("9. Filter by customer status (ACTIVE vs SUSPENDED)")
    void testFilterByStatus() throws Exception {
        // ACTIVE filter (4 active customers)
        mockMvc.perform(get("/api/employee/customers")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(4)));

        // SUSPENDED filter (1 suspended customer: Charlie)
        mockMvc.perform(get("/api/employee/customers")
                        .param("status", "SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Charlie Brown")))
                .andExpect(jsonPath("$.content[0].status", is("SUSPENDED")));
    }

    @Test
    @DisplayName("10. Combined search and status filter")
    void testCombinedSearchAndFilter() throws Exception {
        // Charlie matches search "Charlie" and status "SUSPENDED"
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "charlie")
                        .param("status", "SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Charlie Brown")));

        // Charlie does NOT match status "ACTIVE"
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "charlie")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.empty", is(true)));
    }

    @Test
    @DisplayName("11. Empty result handling for non-matching queries")
    void testEmptyResultHandling() throws Exception {
        mockMvc.perform(get("/api/employee/customers")
                        .param("search", "nonexistent_query_xyz")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.empty", is(true)));
    }

    // =========================================================================
    // 3. Pagination Tests
    // =========================================================================

    @Test
    @DisplayName("12. Pagination metadata and navigation (page 0, 1, 2 with size 2)")
    void testPaginationNavigation() throws Exception {
        // Page 0 (size 2) -> elements 1, 2
        mockMvc.perform(get("/api/employee/customers")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.hasPrevious", is(false)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Page 1 (size 2) -> elements 3, 4
        mockMvc.perform(get("/api/employee/customers")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.first", is(false)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.hasPrevious", is(true)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Page 2 (size 2) -> element 5
        mockMvc.perform(get("/api/employee/customers")
                        .param("page", "2")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(2)))
                .andExpect(jsonPath("$.first", is(false)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(true)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // =========================================================================
    // 4. Data Protection & Password Hash Non-Exposure
    // =========================================================================

    @Test
    @DisplayName("13. Password hash is never exposed in customer directory responses")
    void testPasswordHashNotExposed() throws Exception {
        mockMvc.perform(get("/api/employee/customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    // =========================================================================
    // 5. Existing Customer Endpoints Non-Regression
    // =========================================================================

    @Test
    @DisplayName("14. Customer profile endpoint (/api/customers/{id}/profile) continues to work")
    void testCustomerProfileEndpointContinuesToWork() throws Exception {
        mockMvc.perform(get("/api/customers/" + alice.getCustomerUuid() + "/profile")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerUuid", is(alice.getCustomerUuid())))
                .andExpect(jsonPath("$.customer.name", is(alice.getName())));
    }

    @Test
    @DisplayName("15. Customer single entity lookup (/api/customers/{id}) continues to work")
    void testCustomerSingleLookupContinuesToWork() throws Exception {
        mockMvc.perform(get("/api/customers/" + alice.getCustomerUuid())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerUuid", is(alice.getCustomerUuid())))
                .andExpect(jsonPath("$.email", is(alice.getEmail())));
    }
}
