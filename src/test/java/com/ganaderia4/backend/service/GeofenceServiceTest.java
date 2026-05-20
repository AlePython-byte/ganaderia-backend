package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.pattern.strategy.geofence.GeofenceEvaluationStrategy;
import com.ganaderia4.backend.pattern.strategy.geofence.GeofenceStrategyResolver;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeofenceServiceTest {

    private GeofenceService geofenceService;

    @BeforeEach
    void setUp() {
        GeofenceEvaluationStrategy circularStrategy = new com.ganaderia4.backend.pattern.strategy.geofence.CircularGeofenceStrategy();
        GeofenceStrategyResolver resolver = new GeofenceStrategyResolver(List.of(circularStrategy));

        geofenceService = new GeofenceService(
                null,
                null,
                resolver,
                new PaginationService(new PaginationProperties())
        );
    }

    @Test
    void shouldReturnTrueWhenPointIsInsideCircularGeofence() {
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(200.0);

        boolean result = geofenceService.isInsideGeofence(1.0005, 1.0005, geofence);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenPointIsOutsideCircularGeofence() {
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(50.0);

        boolean result = geofenceService.isInsideGeofence(1.01, 1.01, geofence);

        assertFalse(result);
    }

    @Test
    void shouldDeactivateExistingGeofence() {
        GeofenceRepository geofenceRepository = mock(GeofenceRepository.class);
        GeofenceService service = serviceWithRepository(geofenceRepository);
        Geofence geofence = geofence(10L, true, null);

        when(geofenceRepository.findById(10L)).thenReturn(Optional.of(geofence));
        when(geofenceRepository.save(geofence)).thenReturn(geofence);

        var response = service.deactivateGeofence(10L);

        assertFalse(response.getActive());
        verify(geofenceRepository).save(geofence);
    }

    @Test
    void shouldActivateExistingGeofence() {
        GeofenceRepository geofenceRepository = mock(GeofenceRepository.class);
        GeofenceService service = serviceWithRepository(geofenceRepository);
        Geofence geofence = geofence(11L, false, null);

        when(geofenceRepository.findById(11L)).thenReturn(Optional.of(geofence));
        when(geofenceRepository.save(geofence)).thenReturn(geofence);

        var response = service.activateGeofence(11L);

        assertTrue(response.getActive());
        verify(geofenceRepository).save(geofence);
    }

    @Test
    void shouldRejectActivateWhenCowAlreadyHasAnotherActiveGeofence() {
        GeofenceRepository geofenceRepository = mock(GeofenceRepository.class);
        GeofenceService service = serviceWithRepository(geofenceRepository);
        Cow cow = new Cow();
        cow.setId(7L);
        Geofence target = geofence(12L, false, cow);
        Geofence alreadyActive = geofence(13L, true, cow);

        when(geofenceRepository.findById(12L)).thenReturn(Optional.of(target));
        when(geofenceRepository.findByCowAndActive(cow, true)).thenReturn(Optional.of(alreadyActive));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.activateGeofence(12L));

        assertEquals("La vaca ya tiene una geocerca activa asignada", exception.getMessage());
        verify(geofenceRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenGeofenceDoesNotExist() {
        GeofenceRepository geofenceRepository = mock(GeofenceRepository.class);
        GeofenceService service = serviceWithRepository(geofenceRepository);
        when(geofenceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deactivateGeofence(404L));
        verify(geofenceRepository, never()).save(any());
    }

    private GeofenceService serviceWithRepository(GeofenceRepository geofenceRepository) {
        GeofenceEvaluationStrategy circularStrategy = new com.ganaderia4.backend.pattern.strategy.geofence.CircularGeofenceStrategy();
        GeofenceStrategyResolver resolver = new GeofenceStrategyResolver(List.of(circularStrategy));
        CowRepository cowRepository = mock(CowRepository.class);
        return new GeofenceService(
                geofenceRepository,
                cowRepository,
                resolver,
                new PaginationService(new PaginationProperties())
        );
    }

    private Geofence geofence(Long id, Boolean active, Cow cow) {
        Geofence geofence = new Geofence();
        geofence.setId(id);
        geofence.setName("Potrero norte");
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(100.0);
        geofence.setActive(active);
        geofence.setCow(cow);
        return geofence;
    }
}
