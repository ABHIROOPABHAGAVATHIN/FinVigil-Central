package com.finvigil.transaction.controller;

import com.finvigil.transaction.dto.TransactionCreateRequest;
import com.finvigil.transaction.dto.TransactionResponse;
import com.finvigil.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Processing", description = "Endpoints for ingesting financial transactions and querying transaction histories")
@SecurityRequirement(name = "BearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Process transaction", description = "Ingests a financial transaction, updates Redis velocity counters, stores record, and triggers real-time AML evaluation.")
    public ResponseEntity<TransactionResponse> processTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        TransactionResponse response = transactionService.processTransaction(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{idOrUuid}")
    @Operation(summary = "Get transaction details", description = "Retrieves transaction details by database ID or UUID.")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable String idOrUuid) {
        TransactionResponse response = transactionService.getTransactionByIdOrUuid(idOrUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer transactions", description = "Retrieves transaction history for a specific customer by ID or UUID.")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCustomer(@PathVariable String customerId) {
        List<TransactionResponse> responses = transactionService.getTransactionsByCustomerIdOrUuid(customerId);
        return ResponseEntity.ok(responses);
    }
}
