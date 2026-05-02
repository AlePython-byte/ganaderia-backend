package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.CowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(cowRepository.findByInternalCode("INT-001")).thenReturn(Optional.empty());
        when(cowRepository.findAllTokens()).thenReturn(List.of());
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(1L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-001", response.getToken());
        assertEquals("Luna", response.getName());

        ArgumentCaptor<Cow> captor = ArgumentCaptor.forClass(Cow.class);
        verify(cowRepository).save(captor.capture());
        assertEquals("COW-001", captor.getValue().getToken());
    }

    @Test
    void shouldGenerateNextTokenIgnoringNonMatchingExistingTokens() {
        when(cowRepository.findByInternalCode("INT-001")).thenReturn(Optional.empty());
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001", "ABC-999", "COW-002", "cow-003"));
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(2L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-003", response.getToken());
    }

    @Test
    void shouldIgnoreProvidedTokenAndGenerateBackendToken() {
        request.setToken("FRONT-123");
        when(cowRepository.findByInternalCode("INT-001")).thenReturn(Optional.empty());
        when(cowRepository.findAllTokens()).thenReturn(List.of("COW-001"));
        when(cowRepository.save(any(Cow.class))).thenAnswer(invocation -> {
            Cow cow = invocation.getArgument(0);
            cow.setId(3L);
            return cow;
        });

        CowResponseDTO response = cowService.createCow(request);

        assertEquals("COW-002", response.getToken());
    }

    @Test
    void shouldKeepExistingTokenWhenUpdatingWithoutToken() {
        Cow existing = new Cow();
        existing.setId(10L);
        existing.setToken("COW-010");
        existing.setInternalCode("INT-010");
        existing.setName("Vieja");
        existing.setStatus(CowStatus.DENTRO);

        CowRequestDTO updateRequest = new CowRequestDTO();
        updateRequest.setToken("   ");
        updateRequest.setInternalCode("INT-011");
        updateRequest.setName("Nueva");
        updateRequest.setStatus(CowStatus.FUERA);
        updateRequest.setObservations(null);

        when(cowRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(cowRepository.findByInternalCode("INT-011")).thenReturn(Optional.empty());
        when(cowRepository.save(existing)).thenReturn(existing);

        CowResponseDTO response = cowService.updateCow(10L, updateRequest);

        assertEquals("COW-010", response.getToken());
        assertEquals("INT-011", response.getInternalCode());
        assertEquals("FUERA", response.getStatus());
        verify(cowRepository, never()).findByToken(any());
    }

    @Test
    void shouldRejectDuplicateInternalCodeDuringCreate() {
        when(cowRepository.findByInternalCode("INT-001")).thenReturn(Optional.of(new Cow()));

        ConflictException exception = assertThrows(ConflictException.class, () -> cowService.createCow(request));

        assertEquals("Ya existe una vaca con ese codigo interno", exception.getMessage());
        verify(cowRepository, never()).save(any());
    }
}
