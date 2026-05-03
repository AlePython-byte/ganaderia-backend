package com.ganaderia4.backend.config;

import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    public ApplicationRunner bootstrapAdmin(UserRepository userRepository,
                                            PasswordEncoder passwordEncoder,
                                            AdminBootstrapProperties properties) {
        return args -> {
            if (!properties.isEnabled()) {
                log.info("event=bootstrap_admin_disabled");
                return;
            }

            String normalizedEmail = normalizeEmail(properties.getEmail());
            if (!StringUtils.hasText(normalizedEmail)) {
                log.info("event=bootstrap_admin_exists action=skipped reason=missing_email");
                return;
            }

            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (existingUser.isPresent()) {
                handleExistingAdmin(existingUser.get(), userRepository, passwordEncoder, properties, normalizedEmail);
                return;
            }

            createAdmin(userRepository, passwordEncoder, properties, normalizedEmail);
        };
    }

    private void createAdmin(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             AdminBootstrapProperties properties,
                             String normalizedEmail) {
        String normalizedName = normalizeName(properties.getName());
        if (!StringUtils.hasText(normalizedName) || !StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException(
                    "Bootstrap admin enabled but missing required name/password for admin creation"
            );
        }

        User admin = new User();
        admin.setName(normalizedName);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(properties.getPassword()));
        admin.setRole(Role.ADMINISTRADOR);
        admin.setActive(true);

        userRepository.save(admin);
        log.info(
                "event=bootstrap_admin_created email={} role={}",
                OperationalLogSanitizer.maskEmail(normalizedEmail),
                Role.ADMINISTRADOR.name()
        );
    }

    private void handleExistingAdmin(User existingUser,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AdminBootstrapProperties properties,
                                     String normalizedEmail) {
        boolean changed = false;
        List<String> updatedFields = new ArrayList<>();

        if (properties.isUpdateExisting()) {
            String normalizedName = normalizeName(properties.getName());
            if (StringUtils.hasText(normalizedName) && !normalizedName.equals(existingUser.getName())) {
                existingUser.setName(normalizedName);
                updatedFields.add("name");
                changed = true;
            }
            if (existingUser.getRole() != Role.ADMINISTRADOR) {
                existingUser.setRole(Role.ADMINISTRADOR);
                updatedFields.add("role");
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingUser.getActive())) {
                existingUser.setActive(true);
                updatedFields.add("active");
                changed = true;
            }
        }

        if (properties.isResetPassword()) {
            if (!StringUtils.hasText(properties.getPassword())) {
                throw new IllegalStateException(
                        "Bootstrap admin reset password requested but password is missing"
                );
            }
            existingUser.setPassword(passwordEncoder.encode(properties.getPassword()));
            updatedFields.add("password");
            changed = true;
            log.info(
                    "event=bootstrap_admin_password_reset email={}",
                    OperationalLogSanitizer.maskEmail(normalizedEmail)
            );
        }

        if (changed) {
            userRepository.save(existingUser);
            log.info(
                    "event=bootstrap_admin_updated email={} fields={}",
                    OperationalLogSanitizer.maskEmail(normalizedEmail),
                    String.join(",", updatedFields)
            );
            return;
        }

        log.info(
                "event=bootstrap_admin_exists action=skipped email={}",
                OperationalLogSanitizer.maskEmail(normalizedEmail)
        );
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }

        return name.trim();
    }
}
