package com.finvigil.credit.controller;

import com.finvigil.credit.dto.CreditApplicationResponse;
import com.finvigil.credit.dto.CreditApplyRequest;
import com.finvigil.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit")
@Tag(name = "Credit Underwriting", description = "Endpoints for submitting and querying credit applications and ML decisions")
@SecurityRequirement(name = "BearerAuth")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping("/apply")
    @Operation(summary = "Submit credit application", description = "Validates financial inputs, stores application in PENDING status, and initiates ML risk evaluation.")
    public ResponseEntity<CreditApplicationResponse> applyForCredit(@Valid @RequestBody CreditApplyRequest request) {
        CreditApplicationResponse response = creditService.applyForCredit(request);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @GetMapping("/application/{id}")
    @Operation(summary = "Get credit application", description = "Retrieves a credit application and any ML underwriting decision by application ID or UUID.")
    public ResponseEntity<CreditApplicationResponse> getApplicationById(@PathVariable String id) {
        CreditApplicationResponse response = creditService.getApplicationByIdOrUuid(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer credit applications", description = "Retrieves all credit applications submitted by a specific customer.")
    public ResponseEntity<List<CreditApplicationResponse>> getApplicationsByCustomer(@PathVariable String customerId) {
        List<CreditApplicationResponse> responses = creditService.getApplicationsByCustomerIdOrUuid(customerId);
        return ResponseEntity.ok(responses);
    }
}
