package com.finvigil.customer.controller;

import com.finvigil.customer.dto.CustomerCreateRequest;
import com.finvigil.customer.dto.CustomerProfileResponse;
import com.finvigil.customer.dto.CustomerResponse;
import com.finvigil.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "Endpoints for managing customer entities and unified customer profiles")
@SecurityRequirement(name = "BearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create customer profile", description = "Creates a new customer record.")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID or UUID", description = "Retrieves customer information by numeric ID or UUID.")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable String id) {
        CustomerResponse response = customerService.getCustomerByIdOrUuid(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get Unified Customer Profile", description = "Retrieves unified customer intelligence combining identity, credit underwriting, transactions, and AML alerts.")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(@PathVariable String id) {
        CustomerProfileResponse response = customerService.getCustomerProfile(id);
        return ResponseEntity.ok(response);
    }
}
