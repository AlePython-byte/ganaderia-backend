package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollarService {

    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;

    public CollarService(CollarRepository collarRepository, CowRepository cowRepository) {
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
    }

    public CollarResponseDTO createCollar(CollarRequestDTO requestDTO) {
        if (collarRepository.findByIdentifier(requestDTO.getIdentifier()).isPresent()) {
            throw new ConflictException("Ya existe un collar con ese identificador");
        }

        Collar collar = new Collar();
        collar.setIdentifier(requestDTO.getIdentifier());
        collar.setStatus(requestDTO.getStatus());

        if (requestDTO.getCowId() != null) {
            Cow cow = cowRepository.findById(requestDTO.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

            if (collarRepository.findByCow(cow).isPresent()) {
                throw new ConflictException("La vaca ya tiene un collar asociado");
            }

            collar.setCow(cow);
        }

        Collar savedCollar = collarRepository.save(collar);
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

    public CollarResponseDTO getCollarByIdentifier(String identifier) {
        Collar collar = collarRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no encontrado con ese identificador"));

        return mapToResponseDTO(collar);
    }

    private CollarResponseDTO mapToResponseDTO(Collar collar) {
        Long cowId = null;
        String cowIdentifier = null;
        String cowName = null;

        if (collar.getCow() != null) {
            cowId = collar.getCow().getId();
            cowIdentifier = collar.getCow().getIdentifier();
            cowName = collar.getCow().getName();
        }

        return new CollarResponseDTO(
                collar.getId(),
                collar.getIdentifier(),
                collar.getStatus().name(),
                cowId,
                cowIdentifier,
                cowName
        );
    }
}