package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.UserNotificationPreferenceRequestDTO;
import com.ganaderia4.backend.dto.UserNotificationPreferenceResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.model.UserNotificationPreference;
import com.ganaderia4.backend.repository.UserNotificationPreferenceRepository;
import com.ganaderia4.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserNotificationPreferenceServiceTest {

    @Test
    void shouldCreateDefaultPreferencesWhenMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        User user = sampleUser(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserNotificationPreference.class)))
                .thenAnswer(invocation -> {
                    UserNotificationPreference preference = invocation.getArgument(0);
                    preference.setId(100L);
                    return preference;
                });

        UserNotificationPreferenceService service =
                new UserNotificationPreferenceService(preferenceRepository, userRepository);

        UserNotificationPreferenceResponseDTO response = service.getByUserId(10L);

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getUserId());
        assertEquals(true, response.isEmailEnabled());
        assertEquals(false, response.isSmsEnabled());
        assertEquals("MEDIUM", response.getMinimumSeverity());
        verify(preferenceRepository).save(any(UserNotificationPreference.class));
    }

    @Test
    void shouldUpdateExistingPreferences() {
        UserRepository userRepository = mock(UserRepository.class);
        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        User user = sampleUser(10L);
        UserNotificationPreference existing = existingPreference(user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(UserNotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserNotificationPreferenceService service =
                new UserNotificationPreferenceService(preferenceRepository, userRepository);

        UserNotificationPreferenceRequestDTO request = new UserNotificationPreferenceRequestDTO();
        request.setEmailEnabled(false);
        request.setSmsEnabled(true);
        request.setNotificationEmail("  alertas@ganaderia.test ");
        request.setPhoneNumber(" +57 300-123-4567 ");
        request.setMinimumSeverity("high");

        UserNotificationPreferenceResponseDTO response = service.upsert(10L, request);

        assertEquals(false, response.isEmailEnabled());
        assertEquals(true, response.isSmsEnabled());
        assertEquals("alertas@ganaderia.test", response.getNotificationEmail());
        assertEquals("+57 300-123-4567", response.getPhoneNumber());
        assertEquals("HIGH", response.getMinimumSeverity());
    }

    @Test
    void shouldRejectSmsEnabledWithoutPhoneNumber() {
        UserRepository userRepository = mock(UserRepository.class);
        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        User user = sampleUser(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.of(existingPreference(user)));

        UserNotificationPreferenceService service =
                new UserNotificationPreferenceService(preferenceRepository, userRepository);

        UserNotificationPreferenceRequestDTO request = new UserNotificationPreferenceRequestDTO();
        request.setEmailEnabled(true);
        request.setSmsEnabled(true);
        request.setMinimumSeverity("MEDIUM");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.upsert(10L, request));
        assertEquals("phoneNumber es obligatorio cuando smsEnabled=true", ex.getMessage());
    }

    @Test
    void shouldRejectInvalidMinimumSeverity() {
        UserRepository userRepository = mock(UserRepository.class);
        UserNotificationPreferenceRepository preferenceRepository = mock(UserNotificationPreferenceRepository.class);
        User user = sampleUser(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(10L)).thenReturn(Optional.of(existingPreference(user)));

        UserNotificationPreferenceService service =
                new UserNotificationPreferenceService(preferenceRepository, userRepository);

        UserNotificationPreferenceRequestDTO request = new UserNotificationPreferenceRequestDTO();
        request.setEmailEnabled(true);
        request.setSmsEnabled(false);
        request.setMinimumSeverity("URGENT");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.upsert(10L, request));
        assertEquals("minimumSeverity debe ser uno de LOW, MEDIUM, HIGH o CRITICAL", ex.getMessage());
    }

    private User sampleUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario");
        user.setEmail("user@test.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        return user;
    }

    private UserNotificationPreference existingPreference(User user) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setId(200L);
        preference.setUser(user);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(false);
        preference.setMinimumSeverity(com.ganaderia4.backend.model.NotificationSeverity.MEDIUM);
        return preference;
    }
}
