package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollarReportService {

    private final CollarRepository collarRepository;

    public CollarReportService(CollarRepository collarRepository) {
        this.collarRepository = collarRepository;
    }

    public List<OfflineCollarReportDTO> getOfflineCollarsReport() {
        return collarRepository.findByEnabledTrueAndSignalStatusOrderByLastSeenAtAsc(DeviceSignalStatus.SIN_SENAL)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private OfflineCollarReportDTO mapToDto(Collar collar) {
        Long cowId = collar.getCow() != null ? collar.getCow().getId() : null;
        String cowToken = collar.getCow() != null ? collar.getCow().getToken() : null;
        String cowName = collar.getCow() != null ? collar.getCow().getName() : null;

        return new OfflineCollarReportDTO(
                collar.getId(),
                collar.getToken(),
                collar.getStatus() != null ? collar.getStatus().name() : null,
                collar.getEnabled(),
                collar.getBatteryLevel(),
                collar.getSignalStatus() != null ? collar.getSignalStatus().name() : null,
                collar.getLastSeenAt(),
                cowId,
                cowToken,
                cowName
        );
    }
}