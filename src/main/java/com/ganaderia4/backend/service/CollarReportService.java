package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollarReportService {

    private final CollarRepository collarRepository;
    private final long offlineThresholdMinutes;

    public CollarReportService(CollarRepository collarRepository,
                               @Value("${app.device-monitor.offline-threshold-minutes:15}") long offlineThresholdMinutes) {
        this.collarRepository = collarRepository;
        this.offlineThresholdMinutes = offlineThresholdMinutes;
    }

    public List<OfflineCollarReportDTO> getOfflineCollarsReport() {
        LocalDateTime now = LocalDateTime.now();

        return collarRepository.findByEnabledTrueAndSignalStatusOrderByLastSeenAtAsc(DeviceSignalStatus.SIN_SENAL)
                .stream()
                .map(collar -> mapToDto(collar, now))
                .collect(Collectors.toList());
    }

    public List<OfflineCollarReportDTO> getOfflineCollarsStalenessReport() {
        LocalDateTime now = LocalDateTime.now();

        return collarRepository.findByEnabledTrueAndSignalStatus(DeviceSignalStatus.SIN_SENAL)
                .stream()
                .sorted(stalenessComparator(now))
                .map(collar -> mapToDto(collar, now))
                .toList();
    }

    private OfflineCollarReportDTO mapToDto(Collar collar, LocalDateTime now) {
        Long cowId = collar.getCow() != null ? collar.getCow().getId() : null;
        String cowToken = collar.getCow() != null ? collar.getCow().getToken() : null;
        String cowName = collar.getCow() != null ? collar.getCow().getName() : null;
        Long stalenessMinutes = calculateStalenessMinutes(collar.getLastSeenAt(), now);
        String stalenessBucket = determineStalenessBucket(stalenessMinutes);

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
                cowName,
                stalenessMinutes,
                stalenessBucket
        );
    }

    private Comparator<Collar> stalenessComparator(LocalDateTime now) {
        return Comparator
                .comparing((Collar collar) -> collar.getLastSeenAt() == null ? 0 : 1)
                .thenComparing(
                        (Collar collar) -> calculateStalenessMinutes(collar.getLastSeenAt(), now),
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private Long calculateStalenessMinutes(LocalDateTime lastSeenAt, LocalDateTime now) {
        if (lastSeenAt == null) {
            return null;
        }

        return Math.max(0, Duration.between(lastSeenAt, now).toMinutes());
    }

    private String determineStalenessBucket(Long stalenessMinutes) {
        if (stalenessMinutes == null) {
            return "NEVER_REPORTED";
        }

        if (stalenessMinutes >= 360) {
            return "OFFLINE_GT_6H";
        }

        if (stalenessMinutes >= 60) {
            return "OFFLINE_GT_1H";
        }

        if (stalenessMinutes >= offlineThresholdMinutes) {
            return "OFFLINE_GT_THRESHOLD";
        }

        return "OFFLINE_RECENT";
    }
}
