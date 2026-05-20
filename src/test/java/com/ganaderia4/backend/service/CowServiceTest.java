package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CowServiceTest {

    @Mock
    private CowRepository cowRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private PaginationService paginationService = new PaginationService(new PaginationProperties());

    @InjectMocks
    private CowService cowService;

    private CowRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new CowRequestDTO();
        request.setName("Luna");
        request.setStatus(CowStatus.DENTRO);
        request.setInternalCode("INT-001");
        request.setObservations("Observacion");
    }

    @Test
    void shouldCreateCowWithoutTokenAndGenerateFirstAutomaticToken() {
        request.setInternalCode(null);
        when(cowRepository.findAllTokens()).thenReturn(List.of());
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of());
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(1L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-001", response.getToken());
        assertEquals("INT-001", response.getInternalCode());
        assertEquals("Luna", response.getName());

        ArgumentCaptor<Cow> captor = ArgumentCaptor.forClass(Cow.class);
        verify(cowRepository).save(captor.capture());
        assertEquals("COW-001", captor.getValue().getToken());
        assertEquals("INT-001", captor.getValue().getInternalCode());
    }

    @Test
    void shouldGenerateNextTokenIgnoringNonMatchingExistingTokens() {
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001", "ABC-999", "COW-002", "cow-003"));
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of());
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(2L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-003", response.getToken());
    }

    @Test
    void shouldGenerateNextInternalCodeIgnoringNonMatchingExistingCodes() {
        when(cowRepository.findAllTokens()).thenReturn(List.of());
        when(cowRepository.findAllInternalCodes()).thenReturn(Arrays.asList("INT-001", "LEGACY-999", "INT-002", "int-003", null));
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(5L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("INT-003", response.getInternalCode());
    }

    @Test
    void shouldIgnoreProvidedTokenAndInternalCodeAndGenerateBackendValues() {
        request.setToken("FRONT-123");
        request.setInternalCode("FRONT-INT-999");
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001"));
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of("INT-001"));
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(3L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-002", response.getToken());
        assertEquals("INT-002", response.getInternalCode());
    }

    @Test
    void shouldKeepExistingTokenAndInternalCodeWhenUpdatingWithoutThem() {
        Cow existing = new Cow();
        existing.setId(10L);
        existing.setToken("COW-010");
        existing.setInternalCode("INT-010");
        existing.setName("Vieja");
        existing.setStatus(CowStatus.DENTRO);

        CowRequestDTO updateRequest = new CowRequestDTO();
        updateRequest.setToken("   ");
        updateRequest.setName("Nueva");
        updateRequest.setStatus(CowStatus.FUERA);
        updateRequest.setObservations(null);

        when(cowRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(cowRepository.save(existing)).thenReturn(existing);

        CowResponseDTO response = cowService.updateCow(10L, updateRequest);

        assertEquals("COW-010", response.getToken());
        assertEquals("INT-010", response.getInternalCode());
        assertEquals("FUERA", response.getStatus());
        verify(cowRepository, never()).findByToken(any());
        verify(cowRepository, never()).findByInternalCode(any());
    }

    @Test
    void shouldRejectInternalCodeChangeDuringUpdate() {
        Cow existing = new Cow();
        existing.setId(10L);
        existing.setToken("COW-010");
        existing.setInternalCode("INT-010");
        existing.setName("Vieja");
        existing.setStatus(CowStatus.DENTRO);

        CowRequestDTO updateRequest = new CowRequestDTO();
        updateRequest.setInternalCode("INT-011");
        updateRequest.setName("Nueva");
        updateRequest.setStatus(CowStatus.FUERA);

        when(cowRepository.findById(10L)).thenReturn(Optional.of(existing));

        ConflictException exception = assertThrows(ConflictException.class, () -> cowService.updateCow(10L, updateRequest));

        assertEquals("El codigo interno de la vaca es generado por el backend y no puede modificarse", exception.getMessage());
        verify(cowRepository, never()).save(any());
    }

    @Test
    void shouldRetryCreateCowWhenGeneratedTokenConflicts() {
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001"), List.of("COW-001", "COW-002"));
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of("INT-001"));
        when(cowRepository.save(any(Cow.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint on identifier"))
                .thenAnswer(invocation -> {
                    Cow cow = invocation.getArgument(0);
                    cow.setId(4L);
                    return cow;
                });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-003", response.getToken());
        verify(cowRepository, times(2)).save(any(Cow.class));
        verify(cowRepository, times(2)).findAllTokens();
        verify(cowRepository, times(2)).findAllInternalCodes();
    }

    @Test
    void shouldRetryCreateCowWhenGeneratedInternalCodeConflicts() {
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001"));
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of(), List.of("INT-001"));
        when(cowRepository.save(any(Cow.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint on internal_code"))
                .thenAnswer(invocation -> {
                    Cow cow = invocation.getArgument(0);
                    cow.setId(6L);
                    return cow;
                });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-002", response.getToken());
        assertEquals("INT-002", response.getInternalCode());
        verify(cowRepository, times(2)).save(any(Cow.class));
        verify(cowRepository, times(2)).findAllInternalCodes();
    }

    @Test
    void shouldNotRetryCreateCowWhenPersistenceErrorIsNotTokenConflict() {
        when(cowRepository.findAllTokens()).thenReturn(List.of());
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of());
        when(cowRepository.save(any(Cow.class)))
                .thenThrow(new DataIntegrityViolationException("violates foreign key constraint"));

        assertThrows(DataIntegrityViolationException.class, () -> cowService.createCow(request));

        verify(cowRepository).save(any(Cow.class));
        verify(cowRepository).findAllTokens();
        verify(cowRepository).findAllInternalCodes();
    }

    @Test
    void shouldFailCreateCowWhenGeneratedTokenConflictsExhaustRetries() {
        when(cowRepository.findAllTokens()).thenReturn(
                List.of("COW-001"),
                List.of("COW-001", "COW-002"),
                List.of("COW-001", "COW-002", "COW-003")
        );
        when(cowRepository.findAllInternalCodes()).thenReturn(List.of("INT-001"));
        when(cowRepository.save(any(Cow.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint on identifier"));

        ConflictException exception = assertThrows(ConflictException.class, () -> cowService.createCow(request));

        assertEquals("No fue posible generar identificadores unicos para la vaca", exception.getMessage());
        verify(cowRepository, times(3)).save(any(Cow.class));
        verify(cowRepository, times(3)).findAllTokens();
        verify(cowRepository, times(3)).findAllInternalCodes();
    }

    @Test
    void shouldDeactivateCowWithoutDeletingIt() {
        Cow existing = new Cow();
        existing.setId(10L);
        existing.setToken("COW-010");
        existing.setInternalCode("INT-010");
        existing.setName("Luna");
        existing.setStatus(CowStatus.DENTRO);
        existing.setActive(true);

        when(cowRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(cowRepository.save(existing)).thenReturn(existing);

        CowResponseDTO response = cowService.deactivateCow(10L);

        assertFalse(response.getActive());
        verify(cowRepository).save(existing);
        verify(auditLogService).logWithCurrentActor(
                "DEACTIVATE_COW",
                "COW",
                10L,
                "API",
                "Desactivacion operativa de vaca con token COW-010",
                true
        );
    }

    @Test
    void shouldActivateCowWithoutDeletingIt() {
        Cow existing = new Cow();
        existing.setId(11L);
        existing.setToken("COW-011");
        existing.setInternalCode("INT-011");
        existing.setName("Brisa");
        existing.setStatus(CowStatus.FUERA);
        existing.setActive(false);

        when(cowRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(cowRepository.save(existing)).thenReturn(existing);

        CowResponseDTO response = cowService.activateCow(11L);

        assertTrue(response.getActive());
        verify(cowRepository).save(existing);
        verify(auditLogService).logWithCurrentActor(
                "ACTIVATE_COW",
                "COW",
                11L,
                "API",
                "Activacion operativa de vaca con token COW-011",
                true
        );
    }

    @Test
    void shouldRejectDeactivateWhenCowDoesNotExist() {
        when(cowRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cowService.deactivateCow(404L));
        verify(cowRepository, never()).save(any());
    }
}
