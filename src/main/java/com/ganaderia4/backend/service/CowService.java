package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
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
public class CowService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "token", "internalCode", "name", "status", "active");
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 3;
    private static final String COW_TOKEN_PREFIX = "COW-";
    private static final String COW_INTERNAL_CODE_PREFIX = "INT-";
    private static final Pattern GENERATED_TOKEN_PATTERN = Pattern.compile("^" + COW_TOKEN_PREFIX + "(\\d+)$");
    private static final Pattern GENERATED_INTERNAL_CODE_PATTERN = Pattern.compile("^" + COW_INTERNAL_CODE_PREFIX + "(\\d+)$");

    private final CowRepository cowRepository;
    private final CollarRepository collarRepository;
    private final GeofenceRepository geofenceRepository;
    private final LocationRepository locationRepository;
    private final AlertRepository alertRepository;
    private final AuditLogService auditLogService;
    private final PaginationService paginationService;

    public CowService(CowRepository cowRepository,
                      CollarRepository collarRepository,
                      GeofenceRepository geofenceRepository,
                      LocationRepository locationRepository,
                      AlertRepository alertRepository,
                      AuditLogService auditLogService,
                      PaginationService paginationService) {
        this.cowRepository = cowRepository;
        this.collarRepository = collarRepository;
        this.geofenceRepository = geofenceRepository;
        this.locationRepository = locationRepository;
        this.alertRepository = alertRepository;
        this.auditLogService = auditLogService;
        this.paginationService = paginationService;
    }

    @Transactional
    public CowResponseDTO createCow(CowRequestDTO requestDTO) {
        Cow savedCow = createCowWithGeneratedTokenAndRetry(requestDTO);

        auditLogService.logWithCurrentActor(
                "CREATE_COW",
                "COW",
                savedCow.getId(),
                "API",
                "Creacion de vaca con token " + savedCow.getToken(),
                true
        );

        return mapToResponseDTO(savedCow);
    }

    private Cow createCowWithGeneratedTokenAndRetry(CowRequestDTO requestDTO) {
        for (int attempt = 1; attempt <= MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            Cow cow = new Cow();
            cow.setToken(generateCowToken());
            cow.setInternalCode(generateCowInternalCode());
            cow.setName(requestDTO.getName().trim());
            cow.setStatus(requestDTO.getStatus());
            cow.setActive(true);
            cow.setObservations(normalizeNullable(requestDTO.getObservations()));

            try {
                return cowRepository.save(cow);
            } catch (DataIntegrityViolationException ex) {
                if (!isGeneratedCowIdentifierConflict(ex)) {
                    throw ex;
                }
                if (attempt == MAX_TOKEN_GENERATION_ATTEMPTS) {
                    throw new ConflictException("No fue posible generar identificadores unicos para la vaca");
                }
            }
        }

        throw new IllegalStateException("No fue posible generar un token para la vaca");
    }

    @Transactional
    public CowResponseDTO updateCow(Long id, CowRequestDTO requestDTO) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        String requestedToken = normalizeNullable(requestDTO.getToken());
        String newToken = requestedToken != null ? requestedToken : cow.getToken();
        String requestedInternalCode = normalizeNullable(requestDTO.getInternalCode());

        if (requestedToken != null) {
            cowRepository.findByToken(newToken)
                    .filter(existingCow -> !existingCow.getId().equals(cow.getId()))
                    .ifPresent(existingCow -> {
                        throw new ConflictException("Ya existe otra vaca con ese token");
                    });
        }

        if (requestedInternalCode != null && !requestedInternalCode.equals(cow.getInternalCode())) {
            throw new ConflictException("El codigo interno de la vaca es generado por el backend y no puede modificarse");
        }

        cow.setToken(newToken);
        cow.setName(requestDTO.getName().trim());
        cow.setStatus(requestDTO.getStatus());
        cow.setObservations(normalizeNullable(requestDTO.getObservations()));

        Cow updatedCow = cowRepository.save(cow);

        auditLogService.logWithCurrentActor(
                "UPDATE_COW",
                "COW",
                updatedCow.getId(),
                "API",
                "Actualizacion de vaca con token " + updatedCow.getToken(),
                true
        );

        return mapToResponseDTO(updatedCow);
    }

    public List<CowResponseDTO> getAllCows() {
        return cowRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<CowResponseDTO> getCowsPage(CowStatus status, Boolean active, int page, int size, String sort, String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        Page<Cow> cows;
        if (status != null && active != null) {
            cows = cowRepository.findByStatusAndActive(status, active, pageable);
        } else if (status != null) {
            cows = cowRepository.findByStatus(status, pageable);
        } else if (active != null) {
            cows = cowRepository.findByActive(active, pageable);
        } else {
            cows = cowRepository.findAll(pageable);
        }

        return cows.map(this::mapToResponseDTO);
    }

    @Transactional
    public CowResponseDTO deactivateCow(Long id) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        if (Boolean.FALSE.equals(cow.getActive())) {
            return mapToResponseDTO(cow);
        }

        cow.setActive(false);
        Cow updatedCow = cowRepository.save(cow);

        auditLogService.logWithCurrentActor(
                "DEACTIVATE_COW",
                "COW",
                updatedCow.getId(),
                "API",
                "Desactivacion operativa de vaca con token " + updatedCow.getToken(),
                true
        );

        return mapToResponseDTO(updatedCow);
    }

    @Transactional
    public CowResponseDTO activateCow(Long id) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        if (Boolean.TRUE.equals(cow.getActive())) {
            return mapToResponseDTO(cow);
        }

        cow.setActive(true);
        Cow updatedCow = cowRepository.save(cow);

        auditLogService.logWithCurrentActor(
                "ACTIVATE_COW",
                "COW",
                updatedCow.getId(),
                "API",
                "Activacion operativa de vaca con token " + updatedCow.getToken(),
                true
        );

        return mapToResponseDTO(updatedCow);
    }

    @Transactional
    public void deleteCow(Long id) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        if (collarRepository.findByCow(cow).isPresent()) {
            throw new ConflictException("No se puede eliminar la vaca porque tiene un collar asociado");
        }
        if (geofenceRepository.existsByCow(cow)) {
            throw new ConflictException("No se puede eliminar la vaca porque tiene geocercas asociadas");
        }
        if (locationRepository.existsByCow(cow)) {
            throw new ConflictException("No se puede eliminar la vaca porque tiene ubicaciones registradas");
        }
        if (alertRepository.countByCow(cow) > 0) {
            throw new ConflictException("No se puede eliminar la vaca porque tiene alertas asociadas");
        }

        cowRepository.delete(cow);

        auditLogService.logWithCurrentActor(
                "DELETE_COW",
                "COW",
                cow.getId(),
                "API",
                "Eliminacion de vaca con token " + cow.getToken(),
                true
        );
    }

    public CowResponseDTO getCowById(Long id) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        return mapToResponseDTO(cow);
    }

    public List<CowResponseDTO> getCowsByStatus(CowStatus status) {
        return cowRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public CowResponseDTO getCowByToken(String token) {
        Cow cow = cowRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada con ese token"));

        return mapToResponseDTO(cow);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateCowToken() {
        int nextSequence = cowRepository.findAllTokens().stream()
                .map(this::extractGeneratedTokenSequence)
                .filter(sequence -> sequence > 0)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        return COW_TOKEN_PREFIX + String.format("%03d", nextSequence);
    }

    private String generateCowInternalCode() {
        int nextSequence = cowRepository.findAllInternalCodes().stream()
                .map(this::extractGeneratedInternalCodeSequence)
                .filter(sequence -> sequence > 0)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        return COW_INTERNAL_CODE_PREFIX + String.format("%03d", nextSequence);
    }

    private int extractGeneratedTokenSequence(String token) {
        return extractGeneratedSequence(token, GENERATED_TOKEN_PATTERN);
    }

    private int extractGeneratedInternalCodeSequence(String internalCode) {
        return extractGeneratedSequence(internalCode, GENERATED_INTERNAL_CODE_PATTERN);
    }

    private int extractGeneratedSequence(String value, Pattern pattern) {
        if (value == null) {
            return -1;
        }

        Matcher matcher = pattern.matcher(value.trim());
        if (!matcher.matches()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean isGeneratedCowIdentifierConflict(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if ((normalized.contains("identifier") || normalized.contains("internal_code"))
                        && (normalized.contains("duplicate") || normalized.contains("unique"))) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private CowResponseDTO mapToResponseDTO(Cow cow) {
        return new CowResponseDTO(
                cow.getId(),
                cow.getToken(),
                cow.getInternalCode(),
                cow.getName(),
                cow.getStatus().name(),
                cow.getActive(),
                cow.getObservations()
        );
    }
}
