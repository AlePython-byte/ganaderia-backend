package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Ubicaciones", description = "Gestión de ubicaciones GPS")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @Operation(summary = "Registrar ubicación")
    public LocationResponseDTO registerLocation(@Valid @RequestBody LocationRequestDTO requestDTO) {
        return locationService.registerLocation(requestDTO);
    }

    @GetMapping("/cow/{cowId}")
    @Operation(summary = "Historial paginado por vaca")
    public Page<LocationResponseDTO> getLocationHistoryByCow(@PathVariable Long cowId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return locationService.getLocationHistoryByCow(cowId, page, size);
    }

    @GetMapping("/cow/{cowId}/between")
    @Operation(summary = "Historial paginado por vaca entre fechas")
    public Page<LocationResponseDTO> getLocationHistoryByCowAndDates(
            @PathVariable Long cowId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return locationService.getLocationHistoryByCowAndDates(cowId, start, end, page, size);
    }

    @GetMapping("/cow/{cowId}/last")
    @Operation(summary = "Última ubicación de una vaca")
    public LocationResponseDTO getLastLocationByCow(@PathVariable Long cowId) {
        return locationService.getLastLocationByCow(cowId);
    }
}