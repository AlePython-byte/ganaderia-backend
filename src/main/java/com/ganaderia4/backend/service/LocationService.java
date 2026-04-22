package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactory;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactoryProvider;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.builder.LocationResponseDTOBuilder;
import com.ganaderia4.backend.pattern.facade.MonitoringFacade;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private static final Duration MAX_FUTURE_DRIFT = Duration.ofMinutes(5);
    private static final Duration MAX_PAST_AGE = Duration.ofDays(7);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("timestamp");

    private final LocationRepository locationRepository;
    private final CowRepository cowRepository;
    private final MonitoringFacade monitoringFacade;
    private final LocationProcessingFactoryProvider locationProcessingFactoryProvider;
    private final AuditLogService auditLogService;
    private final PaginationService paginationService;

    public LocationService(LocationRepository locationRepository,
                           CowRepository cowRepository,
                           MonitoringFacade monitoringFacade,
                           LocationProcessingFactoryProvider locationProcessingFactoryProvider,
                           AuditLogService auditLogService,
                           PaginationService paginationService) {
        this.locationRepository = locationRepository;
        this.cowRepository = cowRepository;
        this.monitoringFacade = monitoringFacade;
        this.locationProcessingFactoryProvider = locationProcessingFactoryProvider;
        this.auditLogService = auditLogService;
        this.paginationService = paginationService;
    }

    public LocationResponseDTO registerLocation(LocationRequestDTO requestDTO) {
        LocationProcessingFactory<LocationRequestDTO> factory =
                locationProcessingFactoryProvider.getFactory("API");

        LocationCommand command = factory.createCommand(requestDTO);
        LocationResponseDTO response = monitoringFacade.processLocation(command, factory.getValidationChain());

        auditLogService.logWithCurrentActor(
                "REGISTER_LOCATION",
                "LOCATION",
                response.getId(),
                "API",
                "Registro manual/API de ubicación para vaca " + response.getCowToken(),
                true
        );

        return response;
    }

    public LocationResponseDTO registerLocationFromDevice(DeviceLocationPayloadDTO payloadDTO) {
        validateDevicePayload(payloadDTO);

        LocationProcessingFactory<DeviceLocationPayloadDTO> factory =
                locationProcessingFactoryProvider.getFactory("DEVICE");

        LocationCommand command = factory.createCommand(payloadDTO);
        LocationResponseDTO response = monitoringFacade.processLocation(command, factory.getValidationChain());

        log.info(
                "Ubicación registrada desde dispositivo {} para vaca {} en {}",
                maskToken(payloadDTO.getDeviceToken()),
                response.getCowToken(),
                response.getTimestamp()
        );

        auditLogService.log(
                "REGISTER_DEVICE_LOCATION",
                "LOCATION",
                response.getId(),
                payloadDTO.getDeviceToken(),
                "DEVICE",
                "Registro de ubicación desde dispositivo/collar " + payloadDTO.getDeviceToken(),
                true
        );

        return response;
    }

    public Page<LocationResponseDTO> getLocationHistoryByCow(Long cowId, int page, int size) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        PageRequest pageable = paginationService.createPageRequest(page, size, "timestamp", "DESC", ALLOWED_SORT_FIELDS);

        return locationRepository.findByCowOrderByTimestampDesc(cow, pageable)
                .map(this::mapToResponseDTO);
    }

    public Page<LocationResponseDTO> getLocationHistoryByCowAndDates(Long cowId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end,
                                                                     int page,
                                                                     int size) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        PageRequest pageable = paginationService.createPageRequest(page, size, "timestamp", "DESC", ALLOWED_SORT_FIELDS);

        return locationRepository.findByCowAndTimestampBetweenOrderByTimestampDesc(cow, start, end, pageable)
                .map(this::mapToResponseDTO);
    }

    public LocationResponseDTO getLastLocationByCow(Long cowId) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        Location location = locationRepository.findTopByCowOrderByTimestampDesc(cow)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ubicación registrada para esa vaca"));

        return mapToResponseDTO(location);
    }

    private void validateDevicePayload(DeviceLocationPayloadDTO payloadDTO) {
        if (payloadDTO == null) {
            log.warn("Se rechazó un payload de dispositivo nulo");
            throw new BadRequestException("Payload de dispositivo inválido");
        }

        if (payloadDTO.getDeviceToken() == null || payloadDTO.getDeviceToken().isBlank()) {
            log.warn("Se rechazó un payload por token de dispositivo inválido");
            throw new BadRequestException("Token de dispositivo inválido");
        }

        if (payloadDTO.getLat() == null) {
            log.warn("Se rechazó payload del dispositivo {} por latitud nula", maskToken(payloadDTO.getDeviceToken()));
            throw new BadRequestException("La latitud es obligatoria");
        }

        if (payloadDTO.getLon() == null) {
            log.warn("Se rechazó payload del dispositivo {} por longitud nula", maskToken(payloadDTO.getDeviceToken()));
            throw new BadRequestException("La longitud es obligatoria");
        }

        if (payloadDTO.getReportedAt() == null) {
            log.warn("Se rechazó payload del dispositivo {} por timestamp nulo", maskToken(payloadDTO.getDeviceToken()));
            throw new BadRequestException("El timestamp reportado es obligatorio");
        }

        if (payloadDTO.getLat() < -90.0 || payloadDTO.getLat() > 90.0) {
            log.warn(
                    "Se rechazó payload del dispositivo {} por latitud fuera de rango: {}",
                    maskToken(payloadDTO.getDeviceToken()),
                    payloadDTO.getLat()
            );
            throw new BadRequestException("La latitud está fuera de rango");
        }

        if (payloadDTO.getLon() < -180.0 || payloadDTO.getLon() > 180.0) {
            log.warn(
                    "Se rechazó payload del dispositivo {} por longitud fuera de rango: {}",
                    maskToken(payloadDTO.getDeviceToken()),
                    payloadDTO.getLon()
            );
            throw new BadRequestException("La longitud está fuera de rango");
        }

        LocalDateTime now = LocalDateTime.now();

        if (payloadDTO.getReportedAt().isAfter(now.plus(MAX_FUTURE_DRIFT))) {
            log.warn(
                    "Se rechazó payload del dispositivo {} por timestamp futuro excesivo: {}",
                    maskToken(payloadDTO.getDeviceToken()),
                    payloadDTO.getReportedAt()
            );
            throw new BadRequestException("El timestamp reportado no puede estar demasiado en el futuro");
        }

        if (payloadDTO.getReportedAt().isBefore(now.minus(MAX_PAST_AGE))) {
            log.warn(
                    "Se rechazó payload del dispositivo {} por timestamp demasiado antiguo: {}",
                    maskToken(payloadDTO.getDeviceToken()),
                    payloadDTO.getReportedAt()
            );
            throw new BadRequestException("El timestamp reportado es demasiado antiguo para ser procesado");
        }
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "UNKNOWN";
        }

        String trimmed = token.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }

        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private LocationResponseDTO mapToResponseDTO(Location location) {
        return new LocationResponseDTOBuilder()
                .id(location.getId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .timestamp(location.getTimestamp())
                .cowId(location.getCow().getId())
                .cowToken(location.getCow().getToken())
                .cowName(location.getCow().getName())
                .collarToken(location.getCollar() != null ? location.getCollar().getToken() : null)
                .build();
    }
}
