package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AuditLogResponseDTO;
import com.ganaderia4.backend.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<AuditLogResponseDTO> getRecentLogs(@RequestParam(required = false) String action) {
        if (action != null && !action.isBlank()) {
            return auditLogService.getRecentLogsByAction(action);
        }

        return auditLogService.getRecentLogs();
    }
}