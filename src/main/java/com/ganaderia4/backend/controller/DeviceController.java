package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.DeviceLocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
import com.ganaderia4.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
@Tag(name = "device-controller", description = "Endpoints para recepción de ubicaciones desde collares/dispositivos")
public class DeviceController {

    private final LocationService locationService;

    public DeviceController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/locations")
    @Operation(summary = "Registrar ubicación desde un collar/dispositivo")
    public ResponseEntity<LocationResponseDTO> registerLocationFromDevice(
            @Parameter(description = "Token del collar/dispositivo")
            @RequestHeader("X-Device-Token") String deviceToken,
            @Valid @RequestBody DeviceLocationRequestDTO requestDTO
    ) {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new DeviceUnauthorizedException("Token de dispositivo ausente o inválido");
        }

        DeviceLocationPayloadDTO payloadDTO = new DeviceLocationPayloadDTO();
        payloadDTO.setDeviceToken(deviceToken.trim());
        payloadDTO.setLat(requestDTO.getLatitude());
        payloadDTO.setLon(requestDTO.getLongitude());
        payloadDTO.setReportedAt(requestDTO.getTimestamp());

        LocationResponseDTO response = locationService.registerLocationFromDevice(payloadDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}