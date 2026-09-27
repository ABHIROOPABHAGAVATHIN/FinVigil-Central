package com.finvigil.employee.service;

import com.finvigil.common.enums.AuditAction;
import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.common.enums.EmployeeStatus;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.employee.dto.EmployeeAuthResponse;
import com.finvigil.employee.dto.EmployeeCreateRequest;
import com.finvigil.employee.dto.EmployeeLoginRequest;
import com.finvigil.employee.dto.EmployeeResponse;
import com.finvigil.employee.entity.Employee;
import com.finvigil.employee.repository.EmployeeRepository;
import com.finvigil.exception.AppException;
import com.finvigil.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAuthService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAuthService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.employee.default-admin.email:admin@finvigil.internal}")
    private String defaultAdminEmail;

    @Value("${app.employee.default-admin.password:}")
    private String defaultAdminPassword;

    public EmployeeAuthService(EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService,
                               AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    public void setDefaultAdminCredentials(String email, String password) {
        this.defaultAdminEmail = email;
        this.defaultAdminPassword = password;
    }

    /**
     * Bootstraps an initial ADMIN employee on startup if no employees exist in the database.
     * Requires DEFAULT_ADMIN_PASSWORD to be explicitly provided in external configuration.
     * Fails startup with an IllegalStateException if employees table is empty and password is missing.
     */
    @PostConstruct
    @Transactional
    public void initDefaultAdminIfEmpty() {
        if (employeeRepository.count() == 0) {
            if (defaultAdminPassword == null || defaultAdminPassword.trim().isEmpty()) {
                log.error("Failed to bootstrap initial ADMIN employee: employees table is empty, but DEFAULT_ADMIN_PASSWORD is not configured.");
                throw new IllegalStateException(
                        "Initial admin bootstrap required because employees table is empty, but DEFAULT_ADMIN_PASSWORD is not configured. " +
                        "Please set DEFAULT_ADMIN_PASSWORD in environment or configuration."
                );
            }
            log.info("No employees found in database. Bootstrapping default initial ADMIN employee: {}", defaultAdminEmail);
            Employee admin = new Employee(
                    "System Administrator",
                    defaultAdminEmail.toLowerCase().trim(),
                    passwordEncoder.encode(defaultAdminPassword.trim()),
                    EmployeeRole.ADMIN,
                    EmployeeStatus.ACTIVE
            );
            Employee saved = employeeRepository.save(admin);
            auditLogService.logEvent(
                    "AUTH",
                    "EMPLOYEE",
                    saved.getEmployeeUuid(),
                    AuditAction.EMPLOYEE_REGISTERED.name()
            );
            log.info("Default initial ADMIN employee successfully bootstrapped (UUID: {}).", saved.getEmployeeUuid());
        }
    }

    /**
     * Authenticates an employee and issues an employee JWT carrying role authorization.
     */
    @Transactional
    public EmployeeAuthResponse login(EmployeeLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Employee employee = employeeRepository.findByEmail(email).orElse(null);

        if (employee == null) {
            auditLogService.logEvent(
                    "AUTH",
                    "EMPLOYEE",
                    email,
                    AuditAction.EMPLOYEE_LOGIN_FAILED.name()
            );
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            auditLogService.logEvent(
                    "AUTH",
                    "EMPLOYEE",
                    employee.getEmployeeUuid(),
                    AuditAction.EMPLOYEE_LOGIN_FAILED.name()
            );
            throw new BadCredentialsException("Invalid email or password");
        }

        if (employee.getStatus() == EmployeeStatus.SUSPENDED) {
            auditLogService.logEvent(
                    "AUTH",
                    "EMPLOYEE",
                    employee.getEmployeeUuid(),
                    "EMPLOYEE_LOGIN_BLOCKED_SUSPENDED"
            );
            throw new AppException("Employee account is suspended. Contact compliance administration.",
                    HttpStatus.FORBIDDEN, "EMPLOYEE_ACCOUNT_SUSPENDED");
        }

        auditLogService.logEvent(
                "AUTH",
                "EMPLOYEE",
                employee.getEmployeeUuid(),
                AuditAction.EMPLOYEE_LOGIN.name()
        );

        String token = jwtService.generateEmployeeToken(employee.getEmail(), employee.getEmployeeUuid(), employee.getRole());

        return new EmployeeAuthResponse(
                token,
                employee.getEmployeeUuid(),
                employee.getEmail(),
                employee.getName(),
                employee.getRole(),
                jwtExpirationMs
        );
    }

    /**
     * Internal employee creation (Admin only).
     */
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (employeeRepository.existsByEmail(email)) {
            throw new AppException("Employee email already exists: " + email,
                    HttpStatus.CONFLICT, "DUPLICATE_EMPLOYEE_EMAIL");
        }

        Employee employee = new Employee(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getRole(),
                EmployeeStatus.ACTIVE
        );

        Employee saved = employeeRepository.save(employee);

        auditLogService.logEvent(
                "AUTH",
                "EMPLOYEE",
                saved.getEmployeeUuid(),
                AuditAction.EMPLOYEE_REGISTERED.name()
        );

        return EmployeeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new AppException("Employee not found", HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
        return EmployeeResponse.fromEntity(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByUuid(String employeeUuid) {
        Employee employee = employeeRepository.findByEmployeeUuid(employeeUuid)
                .orElseThrow(() -> new AppException("Employee not found", HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
        return EmployeeResponse.fromEntity(employee);
    }
}
