package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class UserNotificationPreferenceResponseDTO {

    private Long id;
    private Long userId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private String notificationEmail;
    private String phoneNumber;
    private String minimumSeverity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserNotificationPreferenceResponseDTO(Long id,
                                                 Long userId,
                                                 boolean emailEnabled,
                                                 boolean smsEnabled,
                                                 String notificationEmail,
                                                 String phoneNumber,
                                                 String minimumSeverity,
                                                 LocalDateTime createdAt,
                                                 LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.notificationEmail = notificationEmail;
        this.phoneNumber = phoneNumber;
        this.minimumSeverity = minimumSeverity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMinimumSeverity() {
        return minimumSeverity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
