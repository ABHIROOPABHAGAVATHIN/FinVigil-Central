package com.finvigil.aml.dto;

import com.finvigil.common.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;

public class AlertStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private AlertStatus status;

    private String resolutionNotes;

    public AlertStatusUpdateRequest() {
    }

    public AlertStatusUpdateRequest(AlertStatus status, String resolutionNotes) {
        this.status = status;
        this.resolutionNotes = resolutionNotes;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
