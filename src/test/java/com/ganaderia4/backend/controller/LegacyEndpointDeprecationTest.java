package com.ganaderia4.backend.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyEndpointDeprecationTest {

    @Test
    void shouldMarkLegacyListEndpointsAsDeprecated() throws NoSuchMethodException {
        assertDeprecated(AlertController.class.getMethod("getAllAlerts"));
        assertDeprecated(AlertController.class.getMethod("getAlertsByStatus", com.ganaderia4.backend.model.AlertStatus.class));
        assertDeprecated(AlertController.class.getMethod("getAlertsByType", com.ganaderia4.backend.model.AlertType.class));
        assertDeprecated(CollarController.class.getMethod("getAllCollars"));
        assertDeprecated(CollarController.class.getMethod("getCollarsByStatus", com.ganaderia4.backend.model.CollarStatus.class));
        assertDeprecated(CowController.class.getMethod("getAllCows"));
        assertDeprecated(CowController.class.getMethod("getCowsByStatus", com.ganaderia4.backend.model.CowStatus.class));
        assertDeprecated(GeofenceController.class.getMethod("getAllGeofences"));
        assertDeprecated(GeofenceController.class.getMethod("getGeofencesByActive", Boolean.class));
        assertDeprecated(UserController.class.getMethod("getAllUsers"));
        assertDeprecated(UserController.class.getMethod("getUsersByActive", Boolean.class));
        assertDeprecated(ReportController.class.getMethod(
                "getAlertReport",
                java.time.LocalDateTime.class,
                java.time.LocalDateTime.class,
                com.ganaderia4.backend.model.AlertType.class,
                com.ganaderia4.backend.model.AlertStatus.class
        ));
    }

    private void assertDeprecated(Method method) {
        assertTrue(method.isAnnotationPresent(Deprecated.class), () -> method + " debe estar marcado como deprecated");
    }
}
