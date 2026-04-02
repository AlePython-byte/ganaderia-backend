package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@Tag(name = "Geocercas", description = "Gestión de geocercas")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @PostMapping
    @Operation(summary = "Crear geocerca")
    public GeofenceResponseDTO createGeofence(@Valid @RequestBody GeofenceRequestDTO requestDTO) {
        return geofenceService.createGeofence(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Listar geocercas")
    public List<GeofenceResponseDTO> getAllGeofences() {
        return geofenceService.getAllGeofences();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar geocerca por id")
    public GeofenceResponseDTO getGeofenceById(@PathVariable Long id) {
        return geofenceService.getGeofenceById(id);
    }

    @GetMapping("/active/{active}")
    @Operation(summary = "Listar geocercas por estado activo")
    public List<GeofenceResponseDTO> getGeofencesByActive(@PathVariable Boolean active) {
        return geofenceService.getGeofencesByActive(active);
    }
}