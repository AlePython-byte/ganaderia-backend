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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LocationService {

    private static final Duration MAX_FUTURE_DRIFT = Duration.ofMinutes(5);
    private static final Duration MAX_PAST_AGE = Duration.ofDays(7);

    private final LocationRepository locationRepository;
    private final CowRepository cowRepository;
    private final MonitoringFacade monitoringFacade;
    private final LocationProcessingFactoryProvider locationProcessingFactoryProvider;
    private final AuditLogService auditLogService;

    public LocationService(LocationRepository locationRepository,
                           CowRepository cowRepository,
                           MonitoringFacade monitoringFacade,
                           LocationProcessingFactoryProvider locationProcessingFactoryProvider,
                           AuditLogService auditLogService) {
        this.locationRepository = locationRepository;
        this.cowRepository = cowRepository;
        this.monitoringFacade = monitoringFacade;
        this.locationProcessingFactoryProvider = locationProcessingFactoryProvider;
        this.auditLogService = auditLogService;
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

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

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

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

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
            throw new BadRequestException("Payload de dispositivo inválido");
        }

        if (payloadDTO.getDeviceToken() == null || payloadDTO.getDeviceToken().isBlank()) {
            throw new BadRequestException("Token de dispositivo inválido");
        }

        if (payloadDTO.getLat() == null) {
            throw new BadRequestException("La latitud es obligatoria");
        }

        if (payloadDTO.getLon() == null) {
            throw new BadRequestException("La longitud es obligatoria");
        }

        if (payloadDTO.getReportedAt() == null) {
            throw new BadRequestException("El timestamp reportado es obligatorio");
        }

        if (payloadDTO.getLat() < -90.0 || payloadDTO.getLat() > 90.0) {
            throw new BadRequestException("La latitud está fuera de rango");
        }

        if (payloadDTO.getLon() < -180.0 || payloadDTO.getLon() > 180.0) {
            throw new BadRequestException("La longitud está fuera de rango");
        }

        LocalDateTime now = LocalDateTime.now();

        if (payloadDTO.getReportedAt().isAfter(now.plus(MAX_FUTURE_DRIFT))) {
            throw new BadRequestException("El timestamp reportado no puede estar demasiado en el futuro");
        }

        if (payloadDTO.getReportedAt().isBefore(now.minus(MAX_PAST_AGE))) {
            throw new BadRequestException("El timestamp reportado es demasiado antiguo para ser procesado");
        }
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