package com.ganaderia4.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    private String passwordResetUrl = "http://localhost:5173/reset-password";

    public String getPasswordResetUrl() {
        return passwordResetUrl;
    }

    public void setPasswordResetUrl(String passwordResetUrl) {
        this.passwordResetUrl = passwordResetUrl;
    }
}
