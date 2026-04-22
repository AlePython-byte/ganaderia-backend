package com.ganaderia4.backend.dto;

public class DeviceSecretResponseDTO {

    private String deviceToken;
    private String deviceSecret;

    public DeviceSecretResponseDTO() {
    }

    public DeviceSecretResponseDTO(String deviceToken, String deviceSecret) {
        this.deviceToken = deviceToken;
        this.deviceSecret = deviceSecret;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }
}
