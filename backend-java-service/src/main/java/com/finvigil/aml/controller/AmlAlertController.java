package com.finvigil.aml.controller;

import com.finvigil.aml.dto.AlertStatusUpdateRequest;
import com.finvigil.aml.dto.AmlAlertResponse;
import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aml/alerts")
@Tag(name = "AML Alerts", description = "Endpoints for managing, querying, and resolving automated AML compliance alerts")
@SecurityRequirement(name = "BearerAuth")
public class AmlAlertController {

    private final AmlAlertService amlAlertService;

    public AmlAlertController(AmlAlertService amlAlertService) {
        this.amlAlertService = amlAlertService;
    }

    @GetMapping
    @Operation(summary = "List AML alerts",
               description = "Retrieves all AML alerts, with optional filtering by status (OPEN, UNDER_REVIEW, RESOLVED, FALSE_POSITIVE) and risk level.")
    public ResponseEntity<List<AmlAlertResponse>> getAllAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) RiskLevel riskLevel) {
        List<AmlAlertResponse> responses = amlAlertService.getAllAlerts(status, riskLevel);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{alertUuid}")
    @Operation(summary = "Get alert by UUID", description = "Retrieves complete details of an AML alert by its unique UUID.")
    public ResponseEntity<AmlAlertResponse> getAlertByUuid(@PathVariable String alertUuid) {
        AmlAlertResponse response = amlAlertService.getAlertByUuid(alertUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerUuid}")
    @Operation(summary = "Get customer AML alerts", description = "Retrieves all AML alerts associated with a specific customer.")
    public ResponseEntity<List<AmlAlertResponse>> getAlertsByCustomer(@PathVariable String customerUuid) {
        List<AmlAlertResponse> responses = amlAlertService.getAlertsByCustomer(customerUuid);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{alertUuid}/status")
    @Operation(summary = "Update alert status", description = "Updates the operational workflow status and resolution notes of an AML alert.")
    public ResponseEntity<AmlAlertResponse> updateAlertStatus(
            @PathVariable String alertUuid,
            @Valid @RequestBody AlertStatusUpdateRequest request) {
        AmlAlertResponse response = amlAlertService.updateAlertStatus(alertUuid, request);
        return ResponseEntity.ok(response);
    }
}
