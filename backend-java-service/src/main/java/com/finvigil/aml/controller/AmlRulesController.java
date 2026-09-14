package com.finvigil.aml.controller;

import com.finvigil.aml.dto.RuleDefinitionResponse;
import com.finvigil.aml.dto.RuleEvaluationRequest;
import com.finvigil.aml.dto.RuleEvaluationResponse;
import com.finvigil.aml.model.RuleEvaluationResult;
import com.finvigil.aml.model.TransactionEvaluationContext;
import com.finvigil.aml.service.AmlRulesEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/aml/rules")
@Tag(name = "AML Rules Engine", description = "Endpoints for deterministic rule evaluation, compliance checks, and AML policy management")
@SecurityRequirement(name = "BearerAuth")
public class AmlRulesController {

    private final AmlRulesEngine rulesEngine;

    public AmlRulesController(AmlRulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate transaction against AML rules",
               description = "Evaluates transaction attributes and sliding velocity metrics against all active AML compliance rules.")
    public ResponseEntity<RuleEvaluationResponse> evaluateTransaction(@Valid @RequestBody RuleEvaluationRequest request) {
        String txnUuid = request.getTransactionUuid() != null && !request.getTransactionUuid().isBlank()
                ? request.getTransactionUuid()
                : UUID.randomUUID().toString();

        String custUuid = request.getCustomerUuid() != null && !request.getCustomerUuid().isBlank()
                ? request.getCustomerUuid()
                : "test-customer-" + UUID.randomUUID().toString().substring(0, 8);

        TransactionEvaluationContext context = new TransactionEvaluationContext(
                txnUuid,
                custUuid,
                request.getAmount(),
                request.getTransactionType(),
                request.getMerchant(),
                request.getCurrency(),
                request.getTransactionTimestamp(),
                request.getVelocityCount(),
                request.getVelocityAmount()
        );

        RuleEvaluationResult result = rulesEngine.evaluateTransaction(context);
        return ResponseEntity.ok(new RuleEvaluationResponse(result));
    }

    @GetMapping
    @Operation(summary = "List active AML rules",
               description = "Retrieves metadata and descriptions for all registered AML compliance rules.")
    public ResponseEntity<List<RuleDefinitionResponse>> getActiveRules() {
        List<RuleDefinitionResponse> rules = rulesEngine.getActiveRules().stream()
                .map(r -> new RuleDefinitionResponse(r.getRuleName(), r.getRuleType(), r.getDescription(), r.getBaseWeight()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }
}
