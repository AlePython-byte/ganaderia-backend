package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.model.NotificationSeverity;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.model.UserNotificationPreference;
import com.ganaderia4.backend.repository.UserNotificationPreferenceRepository;
import com.ganaderia4.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmailNotificationRecipientResolver {

    private static final List<Role> ELIGIBLE_ROLES = List.of(Role.ADMINISTRADOR, Role.SUPERVISOR);

    private final UserRepository userRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final EmailNotificationProperties properties;

    public EmailNotificationRecipientResolver(UserRepository userRepository,
                                              UserNotificationPreferenceRepository preferenceRepository,
                                              EmailNotificationProperties properties) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.properties = properties;
    }

    public EmailNotificationRecipientsResolution resolveRecipients(NotificationMessage notificationMessage) {
        NotificationSeverity notificationSeverity = resolveNotificationSeverity(notificationMessage);
        List<User> eligibleUsers = userRepository.findByActiveTrueAndRoleIn(ELIGIBLE_ROLES);

        Map<Long, UserNotificationPreference> preferencesByUserId = loadPreferencesByUserId(eligibleUsers);
        LinkedHashMap<String, String> recipientsByLowerCaseEmail = new LinkedHashMap<>();

        for (User user : eligibleUsers) {
            UserNotificationPreference preference = preferencesByUserId.get(user.getId());
            if (!isEligible(preference, notificationSeverity)) {
                continue;
            }

            String resolvedEmail = resolveEmail(user, preference);
            if (resolvedEmail == null) {
                continue;
            }

            recipientsByLowerCaseEmail.putIfAbsent(resolvedEmail.toLowerCase(Locale.ROOT), resolvedEmail);
        }

        if (!recipientsByLowerCaseEmail.isEmpty()) {
            return new EmailNotificationRecipientsResolution(
                    new ArrayList<>(recipientsByLowerCaseEmail.values()),
                    notificationSeverity,
                    false
            );
        }

        List<String> fallbackRecipients = parseFallbackRecipients(properties.getTo());
        return new EmailNotificationRecipientsResolution(
                fallbackRecipients,
                notificationSeverity,
                !fallbackRecipients.isEmpty()
        );
    }

    NotificationSeverity resolveNotificationSeverity(NotificationMessage notificationMessage) {
        if (notificationMessage != null) {
            String severity = normalize(notificationMessage.getSeverity());
            if (!severity.isBlank()) {
                try {
                    return NotificationSeverity.valueOf(severity);
                } catch (IllegalArgumentException ignored) {
                    // Fall back to alert type heuristics below.
                }
            }

            String alertType = normalize(notificationMessage.getMetadata().get("alertType"));
            if ("COLLAR_OFFLINE".equals(alertType) || "EXIT_GEOFENCE".equals(alertType)) {
                return NotificationSeverity.HIGH;
            }
            if ("LOW_BATTERY".equals(alertType)) {
                return NotificationSeverity.MEDIUM;
            }
        }

        return NotificationSeverity.MEDIUM;
    }

    private Map<Long, UserNotificationPreference> loadPreferencesByUserId(List<User> users) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return preferenceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(preference -> preference.getUser().getId(), preference -> preference));
    }

    private boolean isEligible(UserNotificationPreference preference, NotificationSeverity notificationSeverity) {
        boolean emailEnabled = preference == null || preference.isEmailEnabled();
        if (!emailEnabled) {
            return false;
        }

        NotificationSeverity minimumSeverity = preference != null && preference.getMinimumSeverity() != null
                ? preference.getMinimumSeverity()
                : NotificationSeverity.MEDIUM;

        return notificationSeverity.ordinal() >= minimumSeverity.ordinal();
    }

    private String resolveEmail(User user, UserNotificationPreference preference) {
        String preferredEmail = preference != null ? trimToNull(preference.getNotificationEmail()) : null;
        if (preferredEmail != null) {
            return preferredEmail;
        }

        return trimToNull(user.getEmail());
    }

    private List<String> parseFallbackRecipients(String rawRecipients) {
        if (rawRecipients == null || rawRecipients.isBlank()) {
            return List.of();
        }

        LinkedHashMap<String, String> recipients = new LinkedHashMap<>();
        for (String token : rawRecipients.split(",")) {
            String normalized = trimToNull(token);
            if (normalized == null) {
                continue;
            }

            recipients.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
        }

        return new ArrayList<>(recipients.values());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
