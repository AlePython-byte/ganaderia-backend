package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.security.DeviceSigningSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollarServiceTest {

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DeviceSigningSecretService deviceSigningSecretService;

    @Spy
    private PaginationService paginationService = new PaginationService(new PaginationProperties());

    @InjectMocks
    private CollarService collarService;

    @Test
    void shouldRejectChangingCollarTokenThroughGeneralUpdate() {
        Collar collar = new Collar();
        collar.setId(1L);
        collar.setToken("COLLAR-001");
        collar.setStatus(CollarStatus.ACTIVO);

        CollarRequestDTO requestDTO = new CollarRequestDTO();
        requestDTO.setToken("COLLAR-002");
        requestDTO.setStatus(CollarStatus.ACTIVO);

        when(collarRepository.findById(1L)).thenReturn(Optional.of(collar));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> collarService.updateCollar(1L, requestDTO)
        );

        assertEquals(
                "El token del collar es un identificador publico estable y no puede modificarse",
                exception.getMessage()
        );
        verify(collarRepository, never()).save(org.mockito.ArgumentMatchers.any(Collar.class));
        verify(auditLogService, never()).logWithCurrentActor(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean()
        );
    }
}
