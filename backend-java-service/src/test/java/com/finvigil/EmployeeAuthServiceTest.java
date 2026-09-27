package com.finvigil;

import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.common.enums.EmployeeStatus;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.employee.dto.EmployeeAuthResponse;
import com.finvigil.employee.dto.EmployeeCreateRequest;
import com.finvigil.employee.dto.EmployeeLoginRequest;
import com.finvigil.employee.dto.EmployeeResponse;
import com.finvigil.employee.entity.Employee;
import com.finvigil.employee.repository.EmployeeRepository;
import com.finvigil.employee.service.EmployeeAuthService;
import com.finvigil.exception.AppException;
import com.finvigil.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeAuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditLogService auditLogService;

    private PasswordEncoder passwordEncoder;
    private EmployeeAuthService employeeAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        employeeAuthService = new EmployeeAuthService(
                employeeRepository,
                passwordEncoder,
                jwtService,
                auditLogService
        );
    }

    @Test
    @DisplayName("1. Employee creation with BCrypt password hashing")
    void testCreateEmployee_Success() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Sarah Connor",
                "sarah.connor@finvigil.internal",
                "Password@123",
                EmployeeRole.AML_ANALYST
        );

        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(1L);
            emp.setEmployeeUuid("emp-uuid-12345");
            return emp;
        });

        EmployeeResponse response = employeeAuthService.createEmployee(request);

        assertNotNull(response);
        assertEquals("Sarah Connor", response.getName());
        assertEquals("sarah.connor@finvigil.internal", response.getEmail());
        assertEquals(EmployeeRole.AML_ANALYST, response.getRole());
        assertEquals(EmployeeStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        // Verify password is hashed with BCrypt and not stored in plaintext
        assertNotEquals("Password@123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("Password@123", saved.getPasswordHash()));

        verify(auditLogService).logEvent(eq("AUTH"), eq("EMPLOYEE"), eq("emp-uuid-12345"), eq("EMPLOYEE_REGISTERED"));
    }

    @Test
    @DisplayName("2. Employee creation fails on duplicate email")
    void testCreateEmployee_DuplicateEmail() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "Duplicate",
                "existing@finvigil.internal",
                "Password@123",
                EmployeeRole.RISK_ANALYST
        );

        when(employeeRepository.existsByEmail("existing@finvigil.internal")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> employeeAuthService.createEmployee(request));
        assertEquals("DUPLICATE_EMPLOYEE_EMAIL", ex.getErrorCode());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Employee login success with valid credentials")
    void testLogin_Success() {
        String rawPassword = "StrongPassword@2026";
        String hashed = passwordEncoder.encode(rawPassword);

        Employee employee = new Employee("Alice Analyst", "alice@finvigil.internal", hashed, EmployeeRole.CREDIT_ANALYST);
        employee.setEmployeeUuid("emp-uuid-alice");
        employee.setStatus(EmployeeStatus.ACTIVE);

        when(employeeRepository.findByEmail("alice@finvigil.internal")).thenReturn(Optional.of(employee));
        when(jwtService.generateEmployeeToken(anyString(), anyString(), any(EmployeeRole.class))).thenReturn("mock.jwt.token");

        EmployeeLoginRequest request = new EmployeeLoginRequest("alice@finvigil.internal", rawPassword);
        EmployeeAuthResponse response = employeeAuthService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("emp-uuid-alice", response.getEmployeeUuid());
        assertEquals(EmployeeRole.CREDIT_ANALYST, response.getRole());

        verify(jwtService).generateEmployeeToken("alice@finvigil.internal", "emp-uuid-alice", EmployeeRole.CREDIT_ANALYST);
        verify(auditLogService).logEvent(eq("AUTH"), eq("EMPLOYEE"), eq("emp-uuid-alice"), eq("EMPLOYEE_LOGIN"));
    }

    @Test
    @DisplayName("4. Employee login rejected with invalid password")
    void testLogin_InvalidPassword() {
        String hashed = passwordEncoder.encode("CorrectPassword@1");
        Employee employee = new Employee("Bob", "bob@finvigil.internal", hashed, EmployeeRole.VIEWER);
        employee.setEmployeeUuid("emp-uuid-bob");

        when(employeeRepository.findByEmail("bob@finvigil.internal")).thenReturn(Optional.of(employee));

        EmployeeLoginRequest request = new EmployeeLoginRequest("bob@finvigil.internal", "WrongPassword");

        assertThrows(BadCredentialsException.class, () -> employeeAuthService.login(request));

        verify(jwtService, never()).generateEmployeeToken(anyString(), anyString(), any());
        verify(auditLogService).logEvent(eq("AUTH"), eq("EMPLOYEE"), eq("emp-uuid-bob"), eq("EMPLOYEE_LOGIN_FAILED"));
    }

    @Test
    @DisplayName("5. Suspended employee login rejected with 403")
    void testLogin_SuspendedEmployee() {
        String rawPassword = "ValidPassword@1";
        String hashed = passwordEncoder.encode(rawPassword);

        Employee employee = new Employee("Charlie Suspended", "charlie@finvigil.internal", hashed, EmployeeRole.AML_ANALYST);
        employee.setEmployeeUuid("emp-uuid-charlie");
        employee.setStatus(EmployeeStatus.SUSPENDED);

        when(employeeRepository.findByEmail("charlie@finvigil.internal")).thenReturn(Optional.of(employee));

        EmployeeLoginRequest request = new EmployeeLoginRequest("charlie@finvigil.internal", rawPassword);

        AppException ex = assertThrows(AppException.class, () -> employeeAuthService.login(request));
        assertEquals("EMPLOYEE_ACCOUNT_SUSPENDED", ex.getErrorCode());

        verify(jwtService, never()).generateEmployeeToken(anyString(), anyString(), any());
        verify(auditLogService).logEvent(eq("AUTH"), eq("EMPLOYEE"), eq("emp-uuid-charlie"), eq("EMPLOYEE_LOGIN_BLOCKED_SUSPENDED"));
    }

    @Test
    @DisplayName("6. Bootstrap throws IllegalStateException when employees table is empty and password is missing")
    void testInitDefaultAdmin_ThrowsWhenPasswordMissing() {
        when(employeeRepository.count()).thenReturn(0L);
        employeeAuthService.setDefaultAdminCredentials("admin@finvigil.internal", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> employeeAuthService.initDefaultAdminIfEmpty());
        assertTrue(ex.getMessage().contains("DEFAULT_ADMIN_PASSWORD is not configured"));

        // Also verify empty/blank string
        employeeAuthService.setDefaultAdminCredentials("admin@finvigil.internal", "   ");
        IllegalStateException exBlank = assertThrows(IllegalStateException.class, () -> employeeAuthService.initDefaultAdminIfEmpty());
        assertTrue(exBlank.getMessage().contains("DEFAULT_ADMIN_PASSWORD is not configured"));

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Bootstrap succeeds when employees table is empty and password is provided")
    void testInitDefaultAdmin_SuccessWhenPasswordConfigured() {
        when(employeeRepository.count()).thenReturn(0L);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(1L);
            emp.setEmployeeUuid("admin-uuid-bootstrap");
            return emp;
        });

        employeeAuthService.setDefaultAdminCredentials("admin@finvigil.internal", "ExternalAdminSecret123!");
        employeeAuthService.initDefaultAdminIfEmpty();

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertEquals("admin@finvigil.internal", saved.getEmail());
        assertEquals(EmployeeRole.ADMIN, saved.getRole());
        assertEquals(EmployeeStatus.ACTIVE, saved.getStatus());
        assertTrue(passwordEncoder.matches("ExternalAdminSecret123!", saved.getPasswordHash()));

        verify(auditLogService).logEvent(eq("AUTH"), eq("EMPLOYEE"), eq("admin-uuid-bootstrap"), eq("EMPLOYEE_REGISTERED"));
    }

    @Test
    @DisplayName("8. Bootstrap skips when employees already exist even if password is not configured")
    void testInitDefaultAdmin_SkipsWhenEmployeesAlreadyExist() {
        when(employeeRepository.count()).thenReturn(5L);
        employeeAuthService.setDefaultAdminCredentials("admin@finvigil.internal", null);

        assertDoesNotThrow(() -> employeeAuthService.initDefaultAdminIfEmpty());
        verify(employeeRepository, never()).save(any());
        verify(auditLogService, never()).logEvent(anyString(), anyString(), anyString(), anyString());
    }
}

