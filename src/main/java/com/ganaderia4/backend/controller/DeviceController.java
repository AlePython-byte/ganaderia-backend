package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.DeviceLocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.security.DeviceRequestAuthenticationService;
import com.ganaderia4.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/device")
@Tag(name = "device-controller", description = "Endpoints para recepcion de ubicaciones desde collares/dispositivos")
public class DeviceController {

    private final LocationService locationService;
    private final DeviceRequestAuthenticationService deviceRequestAuthenticationService;
    private final Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    public DeviceController(LocationService locationService,
                            DeviceRequestAuthenticationService deviceRequestAuthenticationService,
                            Validator validator) {
        this.locationService = locationService;
        this.deviceRequestAuthenticationService = deviceRequestAuthenticationService;
        this.validator = validator;
    }

    @PostMapping("/locations")
    @Operation(summary = "Registrar ubicacion desde un collar/dispositivo")
    public ResponseEntity<LocationResponseDTO> registerLocationFromDevice(
            @Parameter(description = "Token del collar/dispositivo")
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Parameter(description = "Timestamp ISO-8601 UTC usado para firmar la solicitud")
            @RequestHeader(value = "X-Device-Timestamp", required = false) String deviceTimestamp,
            @Parameter(description = "Nonce unico por solicitud para prevenir replay")
            @RequestHeader(value = "X-Device-Nonce", required = false) String deviceNonce,
            @Parameter(description = "Firma HMAC-SHA256 Base64 de la solicitud canonica")
            @RequestHeader(value = "X-Device-Signature", required = false) String deviceSignature,
            HttpServletRequest httpServletRequest,
            @RequestBody String rawBody
    ) {
        String sanitizedDeviceToken = deviceRequestAuthenticationService.authenticate(
                deviceToken,
                deviceTimestamp,
                deviceNonce,
                deviceSignature,
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                rawBody
        );

        DeviceLocationRequestDTO requestDTO = parseAndValidateRequest(rawBody);

        DeviceLocationPayloadDTO payloadDTO = new DeviceLocationPayloadDTO();
        payloadDTO.setDeviceToken(sanitizedDeviceToken);
        payloadDTO.setLat(requestDTO.getLatitude());
        payloadDTO.setLon(requestDTO.getLongitude());
        payloadDTO.setReportedAt(requestDTO.getTimestamp());

        LocationResponseDTO response = locationService.registerLocationFromDevice(payloadDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private DeviceLocationRequestDTO parseAndValidateRequest(String rawBody) {
        DeviceLocationRequestDTO requestDTO;
        try {
            requestDTO = objectMapper.readValue(rawBody, DeviceLocationRequestDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Payload de dispositivo invalido");
        }

        Set<ConstraintViolation<DeviceLocationRequestDTO>> violations = validator.validate(requestDTO);
        if (!violations.isEmpty()) {
            throw new BadRequestException(violations.iterator().next().getMessage());
        }

        return requestDTO;
    }
}
