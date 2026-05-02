package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.UserNotificationPreferenceRequestDTO;
import com.ganaderia4.backend.dto.UserNotificationPreferenceResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.NotificationSeverity;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.model.UserNotificationPreference;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.UserNotificationPreferenceRepository;
import com.ganaderia4.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserNotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationPreferenceService.class);

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public UserNotificationPreferenceService(UserNotificationPreferenceRepository preferenceRepository,
                                             UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserNotificationPreferenceResponseDTO getByUserId(Long userId) {
        User user = findUser(userId);
        UserNotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(user));

        return mapToResponse(preference);
    }

    @Transactional
    public UserNotificationPreferenceResponseDTO upsert(Long userId, UserNotificationPreferenceRequestDTO requestDTO) {
        User user = findUser(userId);
        UserNotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(user));

        NotificationSeverity minimumSeverity = parseMinimumSeverity(requestDTO.getMinimumSeverity());
        String notificationEmail = trimToNull(requestDTO.getNotificationEmail());
        String phoneNumber = trimToNull(requestDTO.getPhoneNumber());

        if (Boolean.TRUE.equals(requestDTO.getSmsEnabled()) && phoneNumber == null) {
            throw new BadRequestException("phoneNumber es obligatorio cuando smsEnabled=true");
        }

        preference.setEmailEnabled(Boolean.TRUE.equals(requestDTO.getEmailEnabled()));
        preference.setSmsEnabled(Boolean.TRUE.equals(requestDTO.getSmsEnabled()));
        preference.setNotificationEmail(notificationEmail);
        preference.setPhoneNumber(phoneNumber);
        preference.setMinimumSeverity(minimumSeverity);

        UserNotificationPreference savedPreference = preferenceRepository.save(preference);
        logUpdated(savedPreference.getUser().getId());

        return mapToResponse(savedPreference);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private UserNotificationPreference createDefaultPreference(User user) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUser(user);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(false);
        preference.setNotificationEmail(null);
        preference.setPhoneNumber(null);
        preference.setMinimumSeverity(NotificationSeverity.MEDIUM);

        UserNotificationPreference savedPreference = preferenceRepository.save(preference);
        logCreated(user.getId());
        return savedPreference;
    }

    private NotificationSeverity parseMinimumSeverity(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException("minimumSeverity es obligatorio");
        }

        try {
            return NotificationSeverity.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("minimumSeverity debe ser uno de LOW, MEDIUM, HIGH o CRITICAL");
        }
    }

    private UserNotificationPreferenceResponseDTO mapToResponse(UserNotificationPreference preference) {
        return new UserNotificationPreferenceResponseDTO(
                preference.getId(),
                preference.getUser().getId(),
                preference.isEmailEnabled(),
                preference.isSmsEnabled(),
                preference.getNotificationEmail(),
                preference.getPhoneNumber(),
                preference.getMinimumSeverity().name(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }

    private void logCreated(Long userId) {
        logger.info(
                "event=notification_preferences_created requestId={} userId={}",
                OperationalLogSanitizer.requestId(),
                userId
        );
    }

    private void logUpdated(Long userId) {
        logger.info(
                "event=notification_preferences_updated requestId={} userId={}",
                OperationalLogSanitizer.requestId(),
                userId
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
