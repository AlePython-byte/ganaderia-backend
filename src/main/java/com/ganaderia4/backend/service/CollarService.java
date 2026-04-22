package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.dto.DeviceSecretResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.security.DeviceSigningSecretService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollarService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "token", "status", "batteryLevel", "lastSeenAt", "signalStatus", "enabled"
    );

    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final AuditLogService auditLogService;
    private final DeviceSigningSecretService deviceSigningSecretService;
    private final PaginationService paginationService;

    public CollarService(CollarRepository collarRepository,
                         CowRepository cowRepository,
                         AuditLogService auditLogService,
                         DeviceSigningSecretService deviceSigningSecretService,
                         PaginationService paginationService) {
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.auditLogService = auditLogService;
        this.deviceSigningSecretService = deviceSigningSecretService;
        this.paginationService = paginationService;
    }

    @Transactional
    public CollarResponseDTO createCollar(CollarRequestDTO requestDTO) {
        if (collarRepository.findByToken(requestDTO.getToken()).isPresent()) {
            throw new ConflictException("Ya existe un collar con ese token");
        }

        Collar collar = new Collar();
        collar.setToken(requestDTO.getToken().trim());
        collar.setStatus(requestDTO.getStatus());

        if (requestDTO.getCowId() != null) {
            Cow cow = cowRepository.findById(requestDTO.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

            if (collarRepository.findByCow(cow).isPresent()) {
                throw new ConflictException("La vaca ya tiene un collar asociado");
            }

            collar.setCow(cow);
        }

        collar.setBatteryLevel(requestDTO.getBatteryLevel());
        collar.setLastSeenAt(requestDTO.getLastSeenAt());
        collar.setSignalStatus(requestDTO.getSignalStatus() != null ? requestDTO.getSignalStatus() : DeviceSignalStatus.SIN_SENAL);
        collar.setFirmwareVersion(normalizeNullable(requestDTO.getFirmwareVersion()));
        collar.setGpsAccuracy(requestDTO.getGpsAccuracy());
        collar.setEnabled(requestDTO.getEnabled() != null ? requestDTO.getEnabled() : true);
        collar.setNotes(normalizeNullable(requestDTO.getNotes()));

        Collar savedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "CREATE_COLLAR",
                "COLLAR",
                savedCollar.getId(),
                "API",
                "Creación de collar con token " + savedCollar.getToken(),
                true
        );

        return mapToResponseDTO(savedCollar);
    }

    @Transactional
    public CollarResponseDTO updateCollar(Long id, CollarRequestDTO requestDTO) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        String newToken = requestDTO.getToken().trim();

        collarRepository.findByToken(newToken)
                .filter(existingCollar -> !existingCollar.getId().equals(collar.getId()))
                .ifPresent(existingCollar -> {
                    throw new ConflictException("Ya existe otro collar con ese token");
                });

        Cow cow = null;
        if (requestDTO.getCowId() != null) {
            cow = cowRepository.findById(requestDTO.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

            collarRepository.findByCow(cow)
                    .filter(existingCollar -> !existingCollar.getId().equals(collar.getId()))
                    .ifPresent(existingCollar -> {
                        throw new ConflictException("La vaca ya tiene otro collar asociado");
                    });
        }

        collar.setToken(newToken);
        collar.setStatus(requestDTO.getStatus());
        collar.setCow(cow);
        collar.setBatteryLevel(requestDTO.getBatteryLevel());
        collar.setLastSeenAt(requestDTO.getLastSeenAt());
        collar.setSignalStatus(requestDTO.getSignalStatus() != null ? requestDTO.getSignalStatus() : DeviceSignalStatus.SIN_SENAL);
        collar.setFirmwareVersion(normalizeNullable(requestDTO.getFirmwareVersion()));
        collar.setGpsAccuracy(requestDTO.getGpsAccuracy());
        collar.setEnabled(requestDTO.getEnabled() != null ? requestDTO.getEnabled() : true);
        collar.setNotes(normalizeNullable(requestDTO.getNotes()));

        Collar updatedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "UPDATE_COLLAR",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Actualización de collar con token " + updatedCollar.getToken(),
                true
        );

        return mapToResponseDTO(updatedCollar);
    }

    @Transactional
    public CollarResponseDTO enableCollar(Long id) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        if (Boolean.TRUE.equals(collar.getEnabled())) {
            return mapToResponseDTO(collar);
        }

        collar.setEnabled(true);
        Collar updatedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "ENABLE_COLLAR",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Habilitación de collar con token " + updatedCollar.getToken(),
                true
        );

        return mapToResponseDTO(updatedCollar);
    }

    @Transactional
    public CollarResponseDTO disableCollar(Long id) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        if (Boolean.FALSE.equals(collar.getEnabled())) {
            return mapToResponseDTO(collar);
        }

        collar.setEnabled(false);
        Collar updatedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "DISABLE_COLLAR",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Deshabilitación de collar con token " + updatedCollar.getToken(),
                true
        );

        return mapToResponseDTO(updatedCollar);
    }

    @Transactional
    public DeviceSecretResponseDTO rotateDeviceSecret(Long id) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        collar.rotateDeviceSecretSalt();
        Collar updatedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "ROTATE_COLLAR_DEVICE_SECRET",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Rotacion de secreto HMAC de collar " + updatedCollar.getToken(),
                true
        );

        String deviceSecret = deviceSigningSecretService.resolveSigningSecret(updatedCollar.getToken())
                .orElseThrow(() -> new IllegalStateException("No fue posible derivar el secreto del dispositivo"));

        return new DeviceSecretResponseDTO(updatedCollar.getToken(), deviceSecret);
    }

    @Transactional
    public CollarResponseDTO reassignCollar(Long collarId, Long cowId) {
        Collar collar = collarRepository.findById(collarId)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        Cow targetCow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        if (collar.getCow() != null && collar.getCow().getId().equals(targetCow.getId())) {
            return mapToResponseDTO(collar);
        }

        collarRepository.findByCow(targetCow)
                .filter(existingCollar -> !existingCollar.getId().equals(collar.getId()))
                .ifPresent(existingCollar -> {
                    throw new ConflictException("La vaca destino ya tiene otro collar asociado");
                });

        String previousCowToken = collar.getCow() != null ? collar.getCow().getToken() : "SIN_VACA";
        collar.setCow(targetCow);

        Collar updatedCollar = collarRepository.save(collar);

        auditLogService.logWithCurrentActor(
                "REASSIGN_COLLAR",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Reasignación de collar " + updatedCollar.getToken()
                        + " desde " + previousCowToken
                        + " hacia " + targetCow.getToken(),
                true
        );

        return mapToResponseDTO(updatedCollar);
    }

    public List<CollarResponseDTO> getAllCollars() {
        return collarRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<CollarResponseDTO> getCollarsPage(CollarStatus status, int page, int size, String sort, String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        Page<Collar> collars = status != null
                ? collarRepository.findByStatus(status, pageable)
                : collarRepository.findAll(pageable);

        return collars.map(this::mapToResponseDTO);
    }

    public CollarResponseDTO getCollarById(Long id) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        return mapToResponseDTO(collar);
    }

    public List<CollarResponseDTO> getCollarsByStatus(CollarStatus status) {
        return collarRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public CollarResponseDTO getCollarByToken(String token) {
        Collar collar = collarRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado con ese token"));

        return mapToResponseDTO(collar);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CollarResponseDTO mapToResponseDTO(Collar collar) {
        Long cowId = null;
        String cowToken = null;
        String cowName = null;

        if (collar.getCow() != null) {
            cowId = collar.getCow().getId();
            cowToken = collar.getCow().getToken();
            cowName = collar.getCow().getName();
        }

        return new CollarResponseDTO(
                collar.getId(),
                collar.getToken(),
                collar.getStatus().name(),
                cowId,
                cowToken,
                cowName,
                collar.getBatteryLevel(),
                collar.getLastSeenAt(),
                collar.getSignalStatus() != null ? collar.getSignalStatus().name() : null,
                collar.getFirmwareVersion(),
                collar.getGpsAccuracy(),
                collar.getEnabled(),
                collar.getNotes()
        );
    }
}
