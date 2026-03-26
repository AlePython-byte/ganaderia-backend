package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public LocationResponseDTO registerLocation(@Valid @RequestBody LocationRequestDTO requestDTO) {
        return locationService.registerLocation(requestDTO);
    }

    @GetMapping("/cow/{cowId}")
    public List<LocationResponseDTO> getLocationHistoryByCow(@PathVariable Long cowId) {
        return locationService.getLocationHistoryByCow(cowId);
    }

    @GetMapping("/cow/{cowId}/last")
    public LocationResponseDTO getLastLocationByCow(@PathVariable Long cowId) {
        return locationService.getLastLocationByCow(cowId);
    }

    @GetMapping("/cow/{cowId}/range")
    public List<LocationResponseDTO> getLocationHistoryByCowAndDates(
            @PathVariable Long cowId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return locationService.getLocationHistoryByCowAndDates(cowId, start, end);
    }
}