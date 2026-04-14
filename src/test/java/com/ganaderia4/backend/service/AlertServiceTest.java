package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.factory.alert.AlertFactory;
import com.ganaderia4.backend.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ganaderia4.backend.observability.DomainMetricsService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertFactory alertFactory;

    @Mock
    private DomainMetricsService domainMetricsService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AlertService alertService;

    private Cow cow;
    private Location location;

    @BeforeEach
    void setUp() {
        cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");

        location = new Location();
        location.setId(10L);
        location.setCow(cow);
        location.setTimestamp(LocalDateTime.now());
    }

    @Test
    void shouldCreateExitGeofenceAlertWhenNoPendingAlertExists() {
        Alert alertToCreate = new Alert();
        alertToCreate.setType(AlertType.EXIT_GEOFENCE);
        alertToCreate.setStatus(AlertStatus.PENDIENTE);
        alertToCreate.setCow(cow);
        alertToCreate.setLocation(location);
        alertToCreate.setMessage("La vaca VACA-001 salió de la geocerca activa");
        alertToCreate.setCreatedAt(LocalDateTime.now());

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.empty());

        when(alertFactory.createAlert(AlertType.EXIT_GEOFENCE, cow, location)).thenReturn(alertToCreate);

        when(alertRepository.save(alertToCreate)).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(100L);
            return alert;
        });

        Alert created = alertService.createExitGeofenceAlert(cow, location);

        assertNotNull(created);
        assertEquals(AlertType.EXIT_GEOFENCE, created.getType());
        assertEquals(AlertStatus.PENDIENTE, created.getStatus());
        assertEquals(cow, created.getCow());
        assertEquals(location, created.getLocation());
        assertTrue(created.getMessage().contains("VACA-001"));

        verify(alertFactory).createAlert(AlertType.EXIT_GEOFENCE, cow, location);
        verify(alertRepository).save(alertToCreate);
    }

    @Test
    void shouldNotDuplicatePendingExitGeofenceAlert() {
        Alert existingAlert = new Alert();
        existingAlert.setId(200L);
        existingAlert.setCow(cow);
        existingAlert.setLocation(location);
        existingAlert.setType(AlertType.EXIT_GEOFENCE);
        existingAlert.setStatus(AlertStatus.PENDIENTE);

        when(alertRepository.findByCowAndTypeAndStatus(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE
        )).thenReturn(Optional.of(existingAlert));

        Alert result = alertService.createExitGeofenceAlert(cow, location);

        assertNull(result);
        verify(alertFactory, never()).createAlert(any(), any(), any());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldResolveAlert() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setMessage("Alerta de prueba");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCow(cow);
        alert.setLocation(location);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponseDTO response = alertService.resolveAlert(1L, "Caso revisado");

        assertEquals("RESUELTA", response.getStatus());
        assertEquals("Caso revisado", response.getObservations());
        assertEquals("VACA-001", response.getCowToken());

        verify(alertRepository).save(any(Alert.class));
    }
}