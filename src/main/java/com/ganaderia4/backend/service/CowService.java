package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CowService {

    private final CowRepository cowRepository;

    public CowService(CowRepository cowRepository) {
        this.cowRepository = cowRepository;
    }

    public CowResponseDTO createCow(CowRequestDTO requestDTO) {
        if (cowRepository.findByIdentifier(requestDTO.getIdentifier()).isPresent()) {
            throw new ConflictException("Ya existe una vaca con ese identificador");
        }

        if (requestDTO.getInternalCode() != null && !requestDTO.getInternalCode().isBlank()) {
            if (cowRepository.findByInternalCode(requestDTO.getInternalCode()).isPresent()) {
                throw new ConflictException("Ya existe una vaca con ese código interno");
            }
        }

        Cow cow = new Cow();
        cow.setIdentifier(requestDTO.getIdentifier());
        cow.setInternalCode(requestDTO.getInternalCode());
        cow.setName(requestDTO.getName());
        cow.setStatus(requestDTO.getStatus());
        cow.setObservations(requestDTO.getObservations());

        Cow savedCow = cowRepository.save(cow);

        return mapToResponseDTO(savedCow);
    }

    public List<CowResponseDTO> getAllCows() {
        return cowRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
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

    public CowResponseDTO getCowByIdentifier(String identifier) {
        Cow cow = cowRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada con ese identificador"));

        return mapToResponseDTO(cow);
    }

    private CowResponseDTO mapToResponseDTO(Cow cow) {
        return new CowResponseDTO(
                cow.getId(),
                cow.getIdentifier(),
                cow.getInternalCode(),
                cow.getName(),
                cow.getStatus().name(),
                cow.getObservations()
        );
    }
}