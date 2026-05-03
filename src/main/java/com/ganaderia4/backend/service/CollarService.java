package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.dto.DeviceSecretResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.security.DeviceSigningSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CollarService {

    private static final Logger log = LoggerFactory.getLogger(CollarService.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "token", "status", "batteryLevel", "lastSeenAt", "signalStatus", "enabled"
    );
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 3;
    private static final String GENERATED_TOKEN_PREFIX = "COLLAR-";
    private static final Pattern GENERATED_TOKEN_PATTERN = Pattern.compile("^COLLAR-(\\d+)$");

    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final DeviceReplayNonceRepository deviceReplayNonceRepository;
    private final AuditLogService auditLogService;
    private final DeviceSigningSecretService deviceSigningSecretService;
    private final PaginationService paginationService;

    public CollarService(CollarRepository collarRepository,
                         CowRepository cowRepository,
                         DeviceReplayNonceRepository deviceReplayNonceRepository,
                         AuditLogService auditLogService,
                         DeviceSigningSecretService deviceSigningSecretService,
                         PaginationService paginationService) {
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.deviceReplayNonceRepository = deviceReplayNonceRepository;
        this.auditLogService = auditLogService;
        this.deviceSigningSecretService = deviceSigningSecretService;
        this.paginationService = paginationService;
    }

    @Transactional
    public CollarResponseDTO createCollar(CollarRequestDTO requestDTO) {
        Cow cow = null;
        if (requestDTO.getCowId() != null) {
            cow = cowRepository.findById(requestDTO.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

            if (collarRepository.findByCow(cow).isPresent()) {
                throw new ConflictException("La vaca ya tiene un collar asociado");
            }
        }

        Collar savedCollar = createCollarWithGeneratedTokenAndRetry(requestDTO, cow);

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

    private Collar createCollarWithGeneratedTokenAndRetry(CollarRequestDTO requestDTO, Cow cow) {
        for (int attempt = 1; attempt <= MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            Collar collar = new Collar();
            collar.setToken(generateCollarToken());
            collar.setStatus(requestDTO.getStatus());
            collar.setCow(cow);
            collar.setBatteryLevel(requestDTO.getBatteryLevel());
            collar.setLastSeenAt(requestDTO.getLastSeenAt());
            collar.setSignalStatus(requestDTO.getSignalStatus() != null ? requestDTO.getSignalStatus() : DeviceSignalStatus.SIN_SENAL);
            collar.setFirmwareVersion(normalizeNullable(requestDTO.getFirmwareVersion()));
            collar.setGpsAccuracy(requestDTO.getGpsAccuracy());
            collar.setEnabled(requestDTO.getEnabled() != null ? requestDTO.getEnabled() : true);
            collar.setNotes(normalizeNullable(requestDTO.getNotes()));

            try {
                return collarRepository.save(collar);
            } catch (DataIntegrityViolationException ex) {
                if (!isGeneratedTokenConflict(ex)) {
                    throw ex;
                }
                if (attempt == MAX_TOKEN_GENERATION_ATTEMPTS) {
                    throw new ConflictException("No fue posible generar un token unico para el collar");
                }
            }
        }

        throw new IllegalStateException("No fue posible generar un token para el collar");
    }

    @Transactional
    public CollarResponseDTO updateCollar(Long id, CollarRequestDTO requestDTO) {
        Collar collar = collarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado"));

        String newToken = normalizeNullable(requestDTO.getToken());
        if (newToken == null) {
            newToken = collar.getToken();
        }

        if (!collar.getToken().equals(newToken)) {
            throw new BadRequestException("El token del collar es un identificador publico estable y no puede modificarse");
        }

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
                .orElseThrow(() -> {
                    log.warn(
                            "event=collar_secret_rotation_failed requestId={} collarId={} reason={}",
                            OperationalLogSanitizer.requestIdOr("scheduled"),
                            id,
                            "collar_not_found"
                    );
                    return new ResourceNotFoundException("Collar no encontrado");
                });

        collar.rotateDeviceSecretSalt();
        Collar updatedCollar = collarRepository.save(collar);
        int deletedNonces = deviceReplayNonceRepository.deleteByDeviceToken(updatedCollar.getToken());

        auditLogService.logWithCurrentActor(
                "ROTATE_COLLAR_SECRET",
                "COLLAR",
                updatedCollar.getId(),
                "API",
                "Rotacion de secreto HMAC de collar " + OperationalLogSanitizer.maskToken(updatedCollar.getToken()),
                true
        );

        String deviceSecret = deviceSigningSecretService.resolveSigningSecret(updatedCollar.getToken())
                .orElseThrow(() -> {
                    log.warn(
                            "event=collar_secret_rotation_failed requestId={} collarId={} reason={}",
                            OperationalLogSanitizer.requestIdOr("scheduled"),
                            updatedCollar.getId(),
                            "secret_derivation_failed"
                    );
                    return new IllegalStateException("No fue posible derivar el secreto del dispositivo");
                });

        log.info(
                "event=collar_secret_rotation_nonces_invalidated requestId={} collarId={} deleted={}",
                OperationalLogSanitizer.requestIdOr("scheduled"),
                updatedCollar.getId(),
                deletedNonces
        );
        log.info(
                "event=collar_secret_rotation_completed requestId={} collarId={} device={}",
                OperationalLogSanitizer.requestIdOr("scheduled"),
                updatedCollar.getId(),
                OperationalLogSanitizer.maskToken(updatedCollar.getToken())
        );

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

    private String generateCollarToken() {
        int nextSequence = collarRepository.findAllTokens().stream()
                .map(this::extractGeneratedTokenSequence)
                .filter(sequence -> sequence > 0)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        return GENERATED_TOKEN_PREFIX + String.format("%03d", nextSequence);
    }

    private int extractGeneratedTokenSequence(String token) {
        if (token == null) {
            return -1;
        }

        Matcher matcher = GENERATED_TOKEN_PATTERN.matcher(token.trim());
        if (!matcher.matches()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean isGeneratedTokenConflict(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("identifier")
                        && (normalized.contains("duplicate") || normalized.contains("unique"))) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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
