package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.service.GeofenceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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

    @GetMapping("/page")
    public Page<GeofenceResponseDTO> getGeofencesPage(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return geofenceService.getGeofencesPage(active, page, size, sort, direction);
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
