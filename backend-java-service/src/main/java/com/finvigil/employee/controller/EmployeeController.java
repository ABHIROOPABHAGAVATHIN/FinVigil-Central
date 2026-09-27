package com.finvigil.employee.controller;

import com.finvigil.employee.dto.EmployeeAuthResponse;
import com.finvigil.employee.dto.EmployeeCreateRequest;
import com.finvigil.employee.dto.EmployeeLoginRequest;
import com.finvigil.employee.dto.EmployeeResponse;
import com.finvigil.employee.service.EmployeeAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@Tag(name = "Employee & Internal RBAC", description = "Endpoints for employee authentication, internal user administration, and role-based console access")
public class EmployeeController {

    private final EmployeeAuthService employeeAuthService;

    public EmployeeController(EmployeeAuthService employeeAuthService) {
        this.employeeAuthService = employeeAuthService;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Authenticate employee", description = "Verifies employee credentials and issues an employee JWT carrying role authorization.")
    public ResponseEntity<EmployeeAuthResponse> login(@Valid @RequestBody EmployeeLoginRequest request) {
        EmployeeAuthResponse response = employeeAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create employee record", description = "Internal employee provisioning; restricted exclusively to ADMIN role.")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeAuthService.createEmployee(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current employee profile", description = "Returns profile and role information for the currently authenticated employee.")
    public ResponseEntity<EmployeeResponse> getCurrentEmployee(Authentication authentication) {
        String email = authentication.getName();
        EmployeeResponse response = employeeAuthService.getEmployeeByEmail(email);
        return ResponseEntity.ok(response);
    }

    // --- Role-Based Access Control Demonstration Endpoints ---

    @GetMapping("/admin/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin console diagnostic status", description = "Restricted exclusively to ADMIN role.")
    public ResponseEntity<Map<String, Object>> getAdminStatus(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "AUTHORIZED",
                "scope", "ADMIN_CONSOLE",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/risk/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_ANALYST')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Risk analyst console overview", description = "Accessible to ADMIN and RISK_ANALYST roles.")
    public ResponseEntity<Map<String, Object>> getRiskOverview(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "AUTHORIZED",
                "scope", "RISK_CONSOLE",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/aml/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'AML_ANALYST')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "AML compliance console overview", description = "Accessible to ADMIN and AML_ANALYST roles.")
    public ResponseEntity<Map<String, Object>> getAmlOverview(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "AUTHORIZED",
                "scope", "AML_CONSOLE",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/credit/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_ANALYST')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Credit underwriting console overview", description = "Accessible to ADMIN and CREDIT_ANALYST roles.")
    public ResponseEntity<Map<String, Object>> getCreditOverview(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "AUTHORIZED",
                "scope", "CREDIT_CONSOLE",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }
}
