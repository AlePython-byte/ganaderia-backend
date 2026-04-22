package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CowService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "token", "internalCode", "name", "status");

    private final CowRepository cowRepository;
    private final AuditLogService auditLogService;
    private final PaginationService paginationService;

    public CowService(CowRepository cowRepository,
                      AuditLogService auditLogService,
                      PaginationService paginationService) {
        this.cowRepository = cowRepository;
        this.auditLogService = auditLogService;
        this.paginationService = paginationService;
    }

    @Transactional
    public CowResponseDTO createCow(CowRequestDTO requestDTO) {
        if (cowRepository.findByToken(requestDTO.getToken()).isPresent()) {
            throw new ConflictException("Ya existe una vaca con ese token");
        }

        if (requestDTO.getInternalCode() != null && !requestDTO.getInternalCode().isBlank()) {
            if (cowRepository.findByInternalCode(requestDTO.getInternalCode()).isPresent()) {
                throw new ConflictException("Ya existe una vaca con ese código interno");
            }
        }

        Cow cow = new Cow();
        cow.setToken(requestDTO.getToken().trim());
        cow.setInternalCode(normalizeNullable(requestDTO.getInternalCode()));
        cow.setName(requestDTO.getName().trim());
        cow.setStatus(requestDTO.getStatus());
        cow.setObservations(normalizeNullable(requestDTO.getObservations()));

        Cow savedCow = cowRepository.save(cow);

        auditLogService.logWithCurrentActor(
                "CREATE_COW",
                "COW",
                savedCow.getId(),
                "API",
                "Creación de vaca con token " + savedCow.getToken(),
                true
        );

        return mapToResponseDTO(savedCow);
    }

    @Transactional
    public CowResponseDTO updateCow(Long id, CowRequestDTO requestDTO) {
        Cow cow = cowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        String newToken = requestDTO.getToken().trim();
        String newInternalCode = normalizeNullable(requestDTO.getInternalCode());

        cowRepository.findByToken(newToken)
                .filter(existingCow -> !existingCow.getId().equals(cow.getId()))
                .ifPresent(existingCow -> {
                    throw new ConflictException("Ya existe otra vaca con ese token");
                });

        if (newInternalCode != null) {
            cowRepository.findByInternalCode(newInternalCode)
                    .filter(existingCow -> !existingCow.getId().equals(cow.getId()))
                    .ifPresent(existingCow -> {
                        throw new ConflictException("Ya existe otra vaca con ese código interno");
                    });
        }

        cow.setToken(newToken);
        cow.setInternalCode(newInternalCode);
        cow.setName(requestDTO.getName().trim());
        cow.setStatus(requestDTO.getStatus());
        cow.setObservations(normalizeNullable(requestDTO.getObservations()));

        Cow updatedCow = cowRepository.save(cow);

        auditLogService.logWithCurrentActor(
                "UPDATE_COW",
                "COW",
                updatedCow.getId(),
                "API",
                "Actualización de vaca con token " + updatedCow.getToken(),
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

    public Page<CowResponseDTO> getCowsPage(CowStatus status, int page, int size, String sort, String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        Page<Cow> cows = status != null
                ? cowRepository.findByStatus(status, pageable)
                : cowRepository.findAll(pageable);

        return cows.map(this::mapToResponseDTO);
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

    private CowResponseDTO mapToResponseDTO(Cow cow) {
        return new CowResponseDTO(
                cow.getId(),
                cow.getToken(),
                cow.getInternalCode(),
                cow.getName(),
                cow.getStatus().name(),
                cow.getObservations()
        );
    }
}
