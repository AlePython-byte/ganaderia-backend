package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserNotificationPreferenceRequestDTO {

    @NotNull(message = "emailEnabled es obligatorio")
    private Boolean emailEnabled;

    @NotNull(message = "smsEnabled es obligatorio")
    private Boolean smsEnabled;

    @Email(message = "El correo de notificacion no tiene un formato valido")
    @Size(max = 255, message = "El correo de notificacion no puede superar 255 caracteres")
    private String notificationEmail;

    @Size(max = 30, message = "El numero de telefono no puede superar 30 caracteres")
    @Pattern(
            regexp = "^$|^[+0-9][0-9\\- ]{0,29}$",
            message = "El numero de telefono tiene un formato invalido"
    )
    private String phoneNumber;

    @NotBlank(message = "minimumSeverity es obligatorio")
    private String minimumSeverity;

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = trimToNull(notificationEmail);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = trimToNull(phoneNumber);
    }

    public String getMinimumSeverity() {
        return minimumSeverity;
    }

    public void setMinimumSeverity(String minimumSeverity) {
        this.minimumSeverity = minimumSeverity == null ? null : minimumSeverity.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
