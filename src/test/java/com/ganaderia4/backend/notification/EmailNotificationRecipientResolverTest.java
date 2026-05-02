package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.model.NotificationSeverity;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.model.UserNotificationPreference;
import com.ganaderia4.backend.repository.UserNotificationPreferenceRepository;
import com.ganaderia4.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class EmailNotificationRecipientResolverTest {

    @Test
    void shouldResolveAdminAndSupervisorRecipients() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);
        User supervisor = user(2L, "supervisor@test.com", Role.SUPERVISOR, true);
        User tecnico = user(3L, "tecnico@test.com", Role.TECNICO, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin, supervisor));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(
                        preference(admin, true, NotificationSeverity.MEDIUM, null),
                        preference(supervisor, true, NotificationSeverity.MEDIUM, null)
                ));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("admin@test.com", "supervisor@test.com"), result.recipients());
        assertEquals(NotificationSeverity.HIGH, result.severity());
        assertFalse(result.globalFallbackUsed());
        verify(userRepository).findByActiveTrueAndRoleIn(anyList());
        verify(preferenceRepository).findByUserIdIn(anyCollection());
        verifyNoMoreInteractions(userRepository, preferenceRepository);
    }

    @Test
    void shouldNotIncludeTecnicoOrOperadorByDefault() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(preference(admin, true, NotificationSeverity.MEDIUM, null)));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("admin@test.com"), result.recipients());
    }

    @Test
    void shouldSkipInactiveUsersBecauseRepositoryOnlyReturnsActiveOnes() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of());

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection())).thenReturn(List.of());

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of(), result.recipients());
    }

    @Test
    void shouldIgnoreUsersWithEmailDisabled() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(preference(admin, false, NotificationSeverity.LOW, null)));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of(), result.recipients());
    }

    @Test
    void shouldPreferNotificationEmailOverUserEmail() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(preference(admin, true, NotificationSeverity.MEDIUM, "alerts@test.com")));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("alerts@test.com"), result.recipients());
    }

    @Test
    void shouldFallbackToUserEmailWhenNotificationEmailIsEmpty() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(preference(admin, true, NotificationSeverity.MEDIUM, "   ")));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("admin@test.com"), result.recipients());
    }

    @Test
    void shouldDeduplicateRecipientsIgnoringCase() {
        User admin = user(1L, "Admin@Test.com", Role.ADMINISTRADOR, true);
        User supervisor = user(2L, "supervisor@test.com", Role.SUPERVISOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin, supervisor));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(
                        preference(admin, true, NotificationSeverity.MEDIUM, null),
                        preference(supervisor, true, NotificationSeverity.MEDIUM, "admin@test.com")
                ));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("Admin@Test.com"), result.recipients());
    }

    @Test
    void shouldApplyMinimumSeverityRules() {
        User admin = user(1L, "admin@test.com", Role.ADMINISTRADOR, true);
        User supervisor = user(2L, "supervisor@test.com", Role.SUPERVISOR, true);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of(admin, supervisor));

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection()))
                .thenReturn(List.of(
                        preference(admin, true, NotificationSeverity.MEDIUM, null),
                        preference(supervisor, true, NotificationSeverity.HIGH, null)
                ));

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution highResult = resolver.resolveRecipients(highAlert());
        EmailNotificationRecipientsResolution mediumResult = resolver.resolveRecipients(mediumAlert());

        assertEquals(List.of("admin@test.com", "supervisor@test.com"), highResult.recipients());
        assertEquals(List.of("admin@test.com"), mediumResult.recipients());
    }

    @Test
    void shouldUseGlobalFallbackWhenNoPreferenceRecipientsExist() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of());

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection())).thenReturn(List.of());

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("fallback@test.com")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of("fallback@test.com"), result.recipients());
        assertEquals(true, result.globalFallbackUsed());
    }

    @Test
    void shouldReturnNoRecipientsWhenNoPreferencesAndNoFallback() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of());

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection())).thenReturn(List.of());

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(highAlert());

        assertEquals(List.of(), result.recipients());
        assertEquals(false, result.globalFallbackUsed());
    }

    @Test
    void shouldDeriveSeverityFromAlertTypeWhenSeverityIsMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByActiveTrueAndRoleIn(anyList())).thenReturn(List.of());

        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        when(preferenceRepository.findByUserIdIn(anyCollection())).thenReturn(List.of());

        EmailNotificationRecipientResolver resolver = new EmailNotificationRecipientResolver(
                userRepository,
                preferenceRepository,
                emailProperties("")
        );

        NotificationMessage notificationMessage = NotificationMessage.builder()
                .eventType("ALERT_CREATED")
                .message("Bateria baja")
                .metadata("alertType", "LOW_BATTERY")
                .build();

        EmailNotificationRecipientsResolution result = resolver.resolveRecipients(notificationMessage);
        assertEquals(NotificationSeverity.MEDIUM, result.severity());
    }

    private NotificationMessage highAlert() {
        return NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .message("Collar offline")
                .severity("HIGH")
                .metadata("alertType", "COLLAR_OFFLINE")
                .build();
    }

    private NotificationMessage mediumAlert() {
        return NotificationMessage.builder()
                .eventType("ALERT_CREATED")
                .message("Bateria baja")
                .severity("MEDIUM")
                .metadata("alertType", "LOW_BATTERY")
                .build();
    }

    private User user(Long id, String email, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);
        user.setName("User " + id);
        user.setPassword("x");
        return user;
    }

    private UserNotificationPreference preference(User user,
                                                  boolean emailEnabled,
                                                  NotificationSeverity minimumSeverity,
                                                  String notificationEmail) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUser(user);
        preference.setEmailEnabled(emailEnabled);
        preference.setMinimumSeverity(minimumSeverity);
        preference.setNotificationEmail(notificationEmail);
        return preference;
    }

    private EmailNotificationProperties emailProperties(String fallbackTo) {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setTo(fallbackTo);
        return properties;
    }
}
