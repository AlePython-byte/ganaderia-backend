package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.service.GeofenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @PostMapping
    public GeofenceResponseDTO createGeofence(@Valid @RequestBody GeofenceRequestDTO requestDTO) {
        return geofenceService.createGeofence(requestDTO);
    }

    @GetMapping
    public List<GeofenceResponseDTO> getAllGeofences() {
        return geofenceService.getAllGeofences();
    }

    @GetMapping("/{id}")
    public GeofenceResponseDTO getGeofenceById(@PathVariable Long id) {
        return geofenceService.getGeofenceById(id);
    }

    @GetMapping("/active/{active}")
    public List<GeofenceResponseDTO> getGeofencesByActive(@PathVariable Boolean active) {
        return geofenceService.getGeofencesByActive(active);
    }
}