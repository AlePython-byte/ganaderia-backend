package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.pattern.strategy.geofence.GeofenceStrategyResolver;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GeofenceService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "active", "radiusMeters", "centerLatitude", "centerLongitude"
    );

    private final GeofenceRepository geofenceRepository;
    private final CowRepository cowRepository;
    private final GeofenceStrategyResolver geofenceStrategyResolver;
    private final PaginationService paginationService;

    public GeofenceService(GeofenceRepository geofenceRepository,
                           CowRepository cowRepository,
                           GeofenceStrategyResolver geofenceStrategyResolver,
                           PaginationService paginationService) {
        this.geofenceRepository = geofenceRepository;
        this.cowRepository = cowRepository;
        this.geofenceStrategyResolver = geofenceStrategyResolver;
        this.paginationService = paginationService;
    }

    @Transactional
    public GeofenceResponseDTO createGeofence(GeofenceRequestDTO requestDTO) {
        Geofence geofence = new Geofence();
        geofence.setName(requestDTO.getName());
        geofence.setCenterLatitude(requestDTO.getCenterLatitude());
        geofence.setCenterLongitude(requestDTO.getCenterLongitude());
        geofence.setRadiusMeters(requestDTO.getRadiusMeters());
        geofence.setActive(requestDTO.getActive());

        if (requestDTO.getCowId() != null) {
            Cow cow = cowRepository.findById(requestDTO.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

            if (Boolean.TRUE.equals(requestDTO.getActive())
                    && geofenceRepository.findByCowAndActive(cow, true).isPresent()) {
                throw new ConflictException("La vaca ya tiene una geocerca activa asignada");
            }

            geofence.setCow(cow);
        }

        Geofence savedGeofence = geofenceRepository.save(geofence);
        return mapToResponseDTO(savedGeofence);
    }

    public List<GeofenceResponseDTO> getAllGeofences() {
        return geofenceRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<GeofenceResponseDTO> getGeofencesPage(Boolean active, int page, int size, String sort, String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        Page<Geofence> geofences = active != null
                ? geofenceRepository.findByActive(active, pageable)
                : geofenceRepository.findAll(pageable);

        return geofences.map(this::mapToResponseDTO);
    }

    public List<GeofenceResponseDTO> getGeofencesByActive(Boolean active) {
        return geofenceRepository.findByActive(active)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public GeofenceResponseDTO getGeofenceById(Long id) {
        Geofence geofence = geofenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Geocerca no encontrada"));

        return mapToResponseDTO(geofence);
    }

    public boolean isInsideGeofence(Double latitude, Double longitude, Geofence geofence) {
        return geofenceStrategyResolver.resolve(geofence)
                .isInside(latitude, longitude, geofence);
    }

    private GeofenceResponseDTO mapToResponseDTO(Geofence geofence) {
        Long cowId = null;
        String cowToken = null;
        String cowName = null;

        if (geofence.getCow() != null) {
            cowId = geofence.getCow().getId();
            cowToken = geofence.getCow().getToken();
            cowName = geofence.getCow().getName();
        }

        return new GeofenceResponseDTO(
                geofence.getId(),
                geofence.getName(),
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude(),
                geofence.getRadiusMeters(),
                geofence.getActive(),
                cowId,
                cowToken,
                cowName
        );
    }
}
