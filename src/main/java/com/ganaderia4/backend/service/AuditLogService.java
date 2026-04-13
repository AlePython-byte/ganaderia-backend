package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AuditLogResponseDTO;
import com.ganaderia4.backend.model.AuditLog;
import com.ganaderia4.backend.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action,
                    String entityType,
                    Long entityId,
                    String actor,
                    String source,
                    String details,
                    boolean success) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActor(actor);
        auditLog.setSource(source);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLog.setSuccess(success);

        auditLogRepository.save(auditLog);
    }

    public void logWithCurrentActor(String action,
                                    String entityType,
                                    Long entityId,
                                    String source,
                                    String details,
                                    boolean success) {
        log(action, entityType, entityId, getCurrentActor(), source, details, success);
    }

    public List<AuditLogResponseDTO> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponseDTO> getRecentLogsByAction(String action) {
        return auditLogRepository.findTop100ByActionOrderByCreatedAtDesc(action)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private String getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "ANONIMO";
        }

        String name = authentication.getName();

        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return "ANONIMO";
        }

        return name;
    }

    private AuditLogResponseDTO mapToResponseDTO(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getActor(),
                auditLog.getSource(),
                auditLog.getDetails(),
                auditLog.getCreatedAt(),
                auditLog.getSuccess()
        );
    }
}