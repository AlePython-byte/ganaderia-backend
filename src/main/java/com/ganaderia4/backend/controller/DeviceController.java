package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.DeviceLocationRequestDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.security.DeviceAbuseProtectionService;
import com.ganaderia4.backend.security.DeviceRequestAuthenticationService;
import com.ganaderia4.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "Dispositivos", description = "Ingestion de telemetria desde collares con autenticacion HMAC por headers")
public class DeviceController {

    private final LocationService locationService;
    private final DeviceRequestAuthenticationService deviceRequestAuthenticationService;
    private final DeviceAbuseProtectionService deviceAbuseProtectionService;
    private final ClientIpResolver clientIpResolver;
    private final Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    public DeviceController(LocationService locationService,
                            DeviceRequestAuthenticationService deviceRequestAuthenticationService,
                            DeviceAbuseProtectionService deviceAbuseProtectionService,
                            ClientIpResolver clientIpResolver,
                            Validator validator) {
        this.locationService = locationService;
        this.deviceRequestAuthenticationService = deviceRequestAuthenticationService;
        this.deviceAbuseProtectionService = deviceAbuseProtectionService;
        this.clientIpResolver = clientIpResolver;
        this.validator = validator;
    }

    @PostMapping("/locations")
    @Operation(
            summary = "Registrar ubicacion desde un collar",
            description = """
                    Endpoint publico para ingestiones de telemetria desde dispositivos.

                    No usa JWT Bearer. La autenticacion se hace con HMAC por headers:
                    X-Device-Token, X-Device-Timestamp, X-Device-Nonce y X-Device-Signature.

                    El body debe conservarse exactamente igual entre el calculo de la firma y el envio HTTP.
                    El campo timestamp del body usa LocalDateTime sin offset.
                    """
    )
    @SecurityRequirements({
            @SecurityRequirement(name = OpenApiConfig.DEVICE_TOKEN_SCHEME),
            @SecurityRequirement(name = OpenApiConfig.DEVICE_TIMESTAMP_SCHEME),
            @SecurityRequirement(name = OpenApiConfig.DEVICE_NONCE_SCHEME),
            @SecurityRequirement(name = OpenApiConfig.DEVICE_SIGNATURE_SCHEME)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ubicacion registrada correctamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload invalido o validacion de negocio rechazada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Headers HMAC ausentes, expirados o con firma invalida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto logico por idempotencia o duplicado cuando aplique",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Solicitud limitada por rate limiting del canal device",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    public ResponseEntity<LocationResponseDTO> registerLocationFromDevice(
            @Parameter(
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Token publico del collar o dispositivo autorizado"
            )
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Parameter(
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Timestamp ISO-8601 UTC usado para la firma, por ejemplo 2026-04-28T20:52:08Z"
            )
            @RequestHeader(value = "X-Device-Timestamp", required = false) String deviceTimestamp,
            @Parameter(
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Nonce unico por solicitud para proteccion anti-replay"
            )
            @RequestHeader(value = "X-Device-Nonce", required = false) String deviceNonce,
            @Parameter(
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Firma HMAC-SHA256 Base64 de la solicitud canonica"
            )
            @RequestHeader(value = "X-Device-Signature", required = false) String deviceSignature,
            HttpServletRequest httpServletRequest,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload JSON firmado por el dispositivo. El timestamp debe enviarse como yyyy-MM-dd'T'HH:mm:ss.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeviceLocationRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "DeviceLocationRequest",
                                    value = """
                                    {
                                      "latitude": 1.214,
                                      "longitude": -77.281,
                                      "timestamp": "2026-04-28T20:52:08",
                                      "batteryLevel": 18,
                                      "gpsAccuracy": 4.5
                                    }
                                            """
                            )
                    )
            )
            @RequestBody String rawBody
    ) {
        deviceAbuseProtectionService.recordDeviceRequest(
                clientIpResolver.resolve(httpServletRequest),
                deviceToken,
                httpServletRequest.getRequestURI()
        );

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
        payloadDTO.setBatteryLevel(requestDTO.getBatteryLevel());
        payloadDTO.setGpsAccuracy(requestDTO.getGpsAccuracy());

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
