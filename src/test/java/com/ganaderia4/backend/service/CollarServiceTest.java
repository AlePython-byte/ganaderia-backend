package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.DeviceSecretResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import com.ganaderia4.backend.security.DeviceSigningSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CollarServiceTest {

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private DeviceReplayNonceRepository deviceReplayNonceRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DeviceSigningSecretService deviceSigningSecretService;

    @Spy
    private PaginationService paginationService = new PaginationService(new PaginationProperties());

    @InjectMocks
    private CollarService collarService;

    @Test
    void shouldGenerateCollarTokenWhenCreatingWithoutToken() {
        CollarRequestDTO requestDTO = new CollarRequestDTO();
        requestDTO.setStatus(CollarStatus.ACTIVO);
        requestDTO.setSignalStatus(DeviceSignalStatus.MEDIA);
        requestDTO.setEnabled(true);

        when(collarRepository.findAllTokens()).thenReturn(java.util.List.of("COLLAR-001", "LEGACY-ABC"));
        when(collarRepository.save(any(Collar.class))).thenAnswer(invocation -> {
            Collar collar = invocation.getArgument(0);
            collar.setId(10L);
            return collar;
        });

        var response = collarService.createCollar(requestDTO);

        assertEquals("COLLAR-002", response.getToken());
        verify(collarRepository).findAllTokens();
    }

    @Test
    void shouldIgnoreIncomingTokenWhenCreatingCollar() {
        CollarRequestDTO requestDTO = new CollarRequestDTO();
        requestDTO.setToken("FRONTEND-TOKEN");
        requestDTO.setStatus(CollarStatus.ACTIVO);

        when(collarRepository.findAllTokens()).thenReturn(java.util.List.of("COLLAR-001"));
        when(collarRepository.save(any(Collar.class))).thenAnswer(invocation -> {
            Collar collar = invocation.getArgument(0);
            collar.setId(11L);
            return collar;
        });

        var response = collarService.createCollar(requestDTO);

        assertEquals("COLLAR-002", response.getToken());
    }

    @Test
    void shouldKeepCurrentTokenWhenUpdatingWithoutToken() {
        Collar collar = new Collar();
        collar.setId(1L);
        collar.setToken("COLLAR-001");
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);

        CollarRequestDTO requestDTO = new CollarRequestDTO();
        requestDTO.setStatus(CollarStatus.INACTIVO);
        requestDTO.setEnabled(true);

        when(collarRepository.findById(1L)).thenReturn(Optional.of(collar));
        when(collarRepository.save(any(Collar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = collarService.updateCollar(1L, requestDTO);

        assertEquals("COLLAR-001", response.getToken());
        assertEquals("INACTIVO", response.getStatus());
    }

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

    @Test
    void shouldRotateDeviceSecretInvalidateNoncesAndAuditWithoutPersistingSecretInClear(CapturedOutput output) {
        Collar collar = new Collar();
        collar.setId(7L);
        collar.setToken("COLLAR-ROTATE-001");
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);
        collar.rotateDeviceSecretSalt();
        String previousSalt = collar.getDeviceSecretSalt();

        when(collarRepository.findById(7L)).thenReturn(Optional.of(collar));
        when(collarRepository.save(any(Collar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceReplayNonceRepository.deleteByDeviceToken("COLLAR-ROTATE-001")).thenReturn(3);
        when(deviceSigningSecretService.resolveSigningSecret("COLLAR-ROTATE-001")).thenReturn(Optional.of("derived-device-secret"));

        DeviceSecretResponseDTO response = collarService.rotateDeviceSecret(7L);

        assertEquals("COLLAR-ROTATE-001", response.getDeviceToken());
        assertEquals("derived-device-secret", response.getDeviceSecret());
        assertNotEquals(previousSalt, collar.getDeviceSecretSalt());
        assertNotEquals("derived-device-secret", collar.getDeviceSecretSalt());
        verify(deviceReplayNonceRepository).deleteByDeviceToken("COLLAR-ROTATE-001");
        verify(deviceSigningSecretService).resolveSigningSecret("COLLAR-ROTATE-001");
        verify(auditLogService).logWithCurrentActor(
                eq("ROTATE_COLLAR_SECRET"),
                eq("COLLAR"),
                eq(7L),
                eq("API"),
                contains("****-001"),
                eq(true)
        );

        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("event=collar_secret_rotation_completed"));
        assertTrue(logs.contains("event=collar_secret_rotation_nonces_invalidated"));
        assertTrue(logs.contains("deleted=3"));
        assertTrue(logs.contains("device=****-001"));
        assertTrue(!logs.contains("derived-device-secret"));
    }

    @Test
    void shouldFailWhenRotatingSecretForUnknownCollar() {
        when(collarRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> collarService.rotateDeviceSecret(99L));

        verify(deviceReplayNonceRepository, never()).deleteByDeviceToken(any());
        verify(deviceSigningSecretService, never()).resolveSigningSecret(any());
        verify(auditLogService, never()).logWithCurrentActor(any(), any(), any(), any(), any(), anyBoolean());
    }
}
