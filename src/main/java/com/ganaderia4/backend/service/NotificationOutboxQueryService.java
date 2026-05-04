package com.ganaderia4.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.NotificationOutboxDetailDTO;
import com.ganaderia4.backend.dto.NotificationOutboxSummaryDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxMessage;
import com.ganaderia4.backend.notification.NotificationOutboxStatus;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationOutboxQueryService {

    private static final int LAST_ERROR_SUMMARY_MAX_LENGTH = 200;
    private static final int PAYLOAD_PREVIEW_MAX_LENGTH = 300;
    private static final int PAYLOAD_VALUE_MAX_LENGTH = 80;
    private static final Set<String> SENSITIVE_PAYLOAD_KEYS = Set.of(
            "authorization", "apikey", "api_key", "jwt", "token", "secret", "password", "hmac", "resettoken", "reset_token"
    );
    private static final Set<String> BODY_KEYS = Set.of("htmlbody", "textbody", "html", "body", "text");

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final PaginationService paginationService;
    private final ObjectMapper objectMapper;

    public NotificationOutboxQueryService(NotificationOutboxRepository notificationOutboxRepository,
                                          PaginationService paginationService,
                                          ObjectMapper objectMapper) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.paginationService = paginationService;
        this.objectMapper = objectMapper;
    }

    public Page<NotificationOutboxSummaryDTO> list(String status,
                                                   String channel,
                                                   int page,
                                                   int size) {
        paginationService.validatePageRequest(page, size);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        NotificationOutboxStatus statusFilter = parseStatus(status);
        NotificationChannel channelFilter = parseChannel(channel);

        Specification<NotificationOutboxMessage> specification = (root, query, cb) -> cb.conjunction();
        if (statusFilter != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), statusFilter));
        }
        if (channelFilter != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("channel"), channelFilter));
        }

        return notificationOutboxRepository.findAll(specification, pageRequest)
                .map(this::toSummaryDto);
    }

    public NotificationOutboxDetailDTO getById(Long id) {
        NotificationOutboxMessage message = notificationOutboxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje de notification outbox no encontrado"));
        return toDetailDto(message);
    }

    private NotificationOutboxSummaryDTO toSummaryDto(NotificationOutboxMessage message) {
        NotificationOutboxSummaryDTO dto = new NotificationOutboxSummaryDTO();
        dto.setId(message.getId());
        dto.setChannel(message.getChannel());
        dto.setStatus(message.getStatus());
        dto.setEventType(message.getEventType());
        dto.setRecipientMasked(maskRecipient(message.getRecipient()));
        dto.setSubject(message.getSubject());
        dto.setAttempts(message.getAttempts());
        dto.setMaxAttempts(message.getMaxAttempts());
        dto.setNextAttemptAt(message.getNextAttemptAt());
        dto.setLastAttemptAt(message.getLastAttemptAt());
        dto.setSentAt(message.getSentAt());
        dto.setFailedAt(message.getFailedAt());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setUpdatedAt(message.getUpdatedAt());
        dto.setLastErrorSummary(truncate(message.getLastError(), LAST_ERROR_SUMMARY_MAX_LENGTH));
        return dto;
    }

    private NotificationOutboxDetailDTO toDetailDto(NotificationOutboxMessage message) {
        NotificationOutboxDetailDTO dto = new NotificationOutboxDetailDTO();
        NotificationOutboxSummaryDTO summary = toSummaryDto(message);
        dto.setId(summary.getId());
        dto.setChannel(summary.getChannel());
        dto.setStatus(summary.getStatus());
        dto.setEventType(summary.getEventType());
        dto.setRecipientMasked(summary.getRecipientMasked());
        dto.setSubject(summary.getSubject());
        dto.setAttempts(summary.getAttempts());
        dto.setMaxAttempts(summary.getMaxAttempts());
        dto.setNextAttemptAt(summary.getNextAttemptAt());
        dto.setLastAttemptAt(summary.getLastAttemptAt());
        dto.setSentAt(summary.getSentAt());
        dto.setFailedAt(summary.getFailedAt());
        dto.setCreatedAt(summary.getCreatedAt());
        dto.setUpdatedAt(summary.getUpdatedAt());
        dto.setLastErrorSummary(summary.getLastErrorSummary());
        dto.setPayloadSize(message.getPayload() != null ? message.getPayload().length() : 0);
        dto.setPayloadPreview(buildPayloadPreview(message.getPayload()));
        return dto;
    }

    private String buildPayloadPreview(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!root.isObject()) {
                return truncate(OperationalLogSanitizer.safe(payload), PAYLOAD_PREVIEW_MAX_LENGTH);
            }

            Map<String, Object> preview = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                String lowerKey = key.toLowerCase(Locale.ROOT);
                if (SENSITIVE_PAYLOAD_KEYS.contains(lowerKey) || BODY_KEYS.contains(lowerKey)) {
                    preview.put(key, "[REDACTED]");
                    continue;
                }

                JsonNode value = field.getValue();
                if (value == null || value.isNull()) {
                    preview.put(key, null);
                } else if (value.isTextual()) {
                    preview.put(key, truncate(value.asText(), PAYLOAD_VALUE_MAX_LENGTH));
                } else {
                    preview.put(key, truncate(value.toString(), PAYLOAD_VALUE_MAX_LENGTH));
                }
            }
            return truncate(objectMapper.writeValueAsString(preview), PAYLOAD_PREVIEW_MAX_LENGTH);
        } catch (JsonProcessingException ex) {
            return truncate(OperationalLogSanitizer.safe(payload), PAYLOAD_PREVIEW_MAX_LENGTH);
        }
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return null;
        }
        return recipient.contains("@")
                ? OperationalLogSanitizer.maskEmail(recipient)
                : OperationalLogSanitizer.maskToken(recipient);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private NotificationOutboxStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return NotificationOutboxStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado de notification outbox invalido");
        }
    }

    private NotificationChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        try {
            return NotificationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Canal de notification outbox invalido");
        }
    }
}
