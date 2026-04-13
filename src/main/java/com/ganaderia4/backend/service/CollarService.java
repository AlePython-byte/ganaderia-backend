package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollarService {

    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final AuditLogService auditLogService;

    public CollarService(CollarRepository collarRepository,
                         CowRepository cowRepository,
                         AuditLogService auditLogService) {
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CollarResponseDTO createCollar(CollarRequestDTO requestDTO) {
        if (collarRepository.findByToken(requestDTO.getToken()).isPresent()) {
            throw new ConflictException("Ya existe un collar con ese token");
        }

        Collar collar = new Collar();
        collar.setToken(requestDTO.getToken());
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
        collar.setFirmwareVersion(requestDTO.getFirmwareVersion());
        collar.setGpsAccuracy(requestDTO.getGpsAccuracy());
        collar.setEnabled(requestDTO.getEnabled() != null ? requestDTO.getEnabled() : true);
        collar.setNotes(requestDTO.getNotes());

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

    public List<CollarResponseDTO> getAllCollars() {
        return collarRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
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