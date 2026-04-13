package com.ganaderia4.backend.exception;

public class DeviceUnauthorizedException extends RuntimeException {

    public DeviceUnauthorizedException(String message) {
        super(message);
    }
}