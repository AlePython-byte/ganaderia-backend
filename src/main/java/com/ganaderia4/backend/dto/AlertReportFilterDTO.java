package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;

import java.time.LocalDateTime;

public class AlertReportFilterDTO {

    private LocalDateTime from;
    private LocalDateTime to;
    private AlertType type;
    private AlertStatus status;

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }
}