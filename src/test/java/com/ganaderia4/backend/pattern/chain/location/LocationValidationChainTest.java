package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.repository.CollarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationValidationChainTest {

    @Mock
    private CollarRepository collarRepository;

    private LocationValidationChain locationValidationChain;

    private Cow cow;
    private Collar collar;

    @BeforeEach
    void setUp() {
        cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");

        collar = new Collar();
        collar.setId(1L);
        collar.setToken("COL-001");
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setCow(cow);

        locationValidationChain = new LocationValidationChain(List.of(
                new TimestampValidationHandler(),
                new CoordinateValidationHandler(),
                new CollarExistsValidationHandler(collarRepository),
                new CollarAssignedValidationHandler(),
                new CollarActiveValidationHandler(),
                new CollarEnabledValidationHandler()
        ));
    }

    @Test
    void shouldPassValidationAndPopulateContext() {
        LocationCommand command = new LocationCommand(
                "COL-001",
                1.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        LocationValidationContext context = new LocationValidationContext(command);

        locationValidationChain.validate(context);

        assertEquals(collar, context.getCollar());
        assertEquals(cow, context.getCow());
    }

    @Test
    void shouldFailWhenCoordinatesAreInvalid() {
        LocationCommand command = new LocationCommand(
                "COL-001",
                100.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(BadRequestException.class, () -> locationValidationChain.validate(context));
    }

    @Test
    void shouldFailWhenTimestampIsInFuture() {
        LocationCommand command = new LocationCommand(
                "COL-001",
                1.0,
                1.0,
                LocalDateTime.now().plusMinutes(10)
        );

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(BadRequestException.class, () -> locationValidationChain.validate(context));
    }

    @Test
    void shouldFailWhenCollarDoesNotExist() {
        LocationCommand command = new LocationCommand(
                "COL-404",
                1.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        when(collarRepository.findByToken("COL-404")).thenReturn(Optional.empty());

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(ResourceNotFoundException.class, () -> locationValidationChain.validate(context));
    }

    @Test
    void shouldFailWhenCollarHasNoCowAssigned() {
        Collar collarWithoutCow = new Collar();
        collarWithoutCow.setId(2L);
        collarWithoutCow.setToken("COL-002");
        collarWithoutCow.setStatus(CollarStatus.ACTIVO);

        LocationCommand command = new LocationCommand(
                "COL-002",
                1.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        when(collarRepository.findByToken("COL-002")).thenReturn(Optional.of(collarWithoutCow));

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(BadRequestException.class, () -> locationValidationChain.validate(context));
    }

    @Test
    void shouldFailWhenCollarIsNotActive() {
        collar.setStatus(CollarStatus.INACTIVO);

        LocationCommand command = new LocationCommand(
                "COL-001",
                1.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(BadRequestException.class, () -> locationValidationChain.validate(context));
    }

    @Test
    void shouldFailWhenCollarIsDisabled() {
        collar.setEnabled(false);

        LocationCommand command = new LocationCommand(
                "COL-001",
                1.0,
                1.0,
                LocalDateTime.now().minusMinutes(1)
        );

        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        LocationValidationContext context = new LocationValidationContext(command);

        assertThrows(BadRequestException.class, () -> locationValidationChain.validate(context));
    }
}
