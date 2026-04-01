package com.ganaderia4.backend.config;

import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    public ApplicationRunner bootstrapAdmin(UserRepository userRepository,
                                            PasswordEncoder passwordEncoder,
                                            @Value("${app.bootstrap.admin.name:}") String adminName,
                                            @Value("${app.bootstrap.admin.email:}") String adminEmail,
                                            @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            if (!StringUtils.hasText(adminName) || !StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
                return;
            }

            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMINISTRADOR);
            admin.setActive(true);

            userRepository.save(admin);
        };
    }
}