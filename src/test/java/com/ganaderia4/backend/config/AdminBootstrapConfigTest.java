package com.ganaderia4.backend.config;

import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AdminBootstrapConfigTest {

    private final AdminBootstrapConfig config = new AdminBootstrapConfig();

    @Test
    void shouldDoNothingWhenBootstrapIsDisabled() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapProperties properties = properties();
        properties.setEnabled(false);

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    void shouldCreateAdminWhenConfiguredEmailDoesNotExist() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AdminBootstrapProperties properties = properties();
        properties.setName("Ganadero Pro");
        properties.setEmail(" Admin@Ganaderia.com ");
        properties.setPassword("secret-123");

        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository).findByEmailIgnoreCase("admin@ganaderia.com");
        verify(userRepository).save(argThat(user ->
                "Ganadero Pro".equals(user.getName())
                        && "admin@ganaderia.com".equals(user.getEmail())
                        && Role.ADMINISTRADOR == user.getRole()
                        && Boolean.TRUE.equals(user.getActive())
                        && passwordEncoder.matches("secret-123", user.getPassword())
        ));
        verify(userRepository, never()).count();
    }

    @Test
    void shouldFailFastWhenAdminMustBeCreatedWithoutPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapProperties properties = properties();
        properties.setName("Ganadero Pro");
        properties.setEmail("admin@ganaderia.com");

        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.empty());

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(exception.getMessage().contains("missing required name/password"));
        verify(userRepository).findByEmailIgnoreCase("admin@ganaderia.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldSkipExistingAdminByDefault() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapProperties properties = properties();
        properties.setName("Otro Nombre");
        properties.setEmail("admin@ganaderia.com");
        properties.setPassword("new-secret");

        User existingUser = user("Admin", "admin@ganaderia.com", "old-hash", Role.ADMINISTRADOR, true);
        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.of(existingUser));

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals("Admin", existingUser.getName());
        assertEquals("old-hash", existingUser.getPassword());
        verify(userRepository).findByEmailIgnoreCase("admin@ganaderia.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldUpdateExistingAdminWhenExplicitlyEnabled() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapProperties properties = properties();
        properties.setName("Ganadero Pro");
        properties.setEmail("admin@ganaderia.com");
        properties.setUpdateExisting(true);

        User existingUser = user("Legacy", "ADMIN@GANADERIA.COM", "old-hash", Role.OPERADOR, false);
        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals("Ganadero Pro", existingUser.getName());
        assertEquals(Role.ADMINISTRADOR, existingUser.getRole());
        assertTrue(existingUser.getActive());
        assertEquals("old-hash", existingUser.getPassword());
        verify(userRepository).save(existingUser);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldResetPasswordWhenExplicitlyEnabled() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AdminBootstrapProperties properties = properties();
        properties.setEmail("admin@ganaderia.com");
        properties.setPassword("new-secret");
        properties.setResetPassword(true);

        User existingUser = user("Admin", "admin@ganaderia.com", "old-hash", Role.ADMINISTRADOR, true);
        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(!"old-hash".equals(existingUser.getPassword()));
        assertTrue(passwordEncoder.matches("new-secret", existingUser.getPassword()));
        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldFailWhenPasswordResetIsRequestedWithoutPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapProperties properties = properties();
        properties.setEmail("admin@ganaderia.com");
        properties.setResetPassword(true);

        User existingUser = user("Admin", "admin@ganaderia.com", "old-hash", Role.ADMINISTRADOR, true);
        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.of(existingUser));

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(exception.getMessage().contains("reset password requested"));
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldCreateAdminEvenWhenOtherUsersExist() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AdminBootstrapProperties properties = properties();
        properties.setName("Ganadero Pro");
        properties.setEmail("admin@ganaderia.com");
        properties.setPassword("secret-123");

        when(userRepository.findByEmailIgnoreCase("admin@ganaderia.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, properties);
        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository).findByEmailIgnoreCase("admin@ganaderia.com");
        verify(userRepository, never()).count();
        verify(userRepository).save(any(User.class));
    }

    private AdminBootstrapProperties properties() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        return properties;
    }

    private User user(String name, String email, String password, Role role, boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
