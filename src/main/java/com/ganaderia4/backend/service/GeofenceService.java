package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final CowRepository cowRepository;

    public GeofenceService(GeofenceRepository geofenceRepository, CowRepository cowRepository) {
        this.geofenceRepository = geofenceRepository;
        this.cowRepository = cowRepository;
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
        double distance = calculateDistanceMeters(
                latitude,
                longitude,
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude()
        );

        return distance <= geofence.getRadiusMeters();
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadius = 6371000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
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