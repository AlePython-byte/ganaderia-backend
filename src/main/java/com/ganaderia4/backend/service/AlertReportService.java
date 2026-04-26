package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertTrendPointDTO;
import com.ganaderia4.backend.dto.AlertTypeRecurrenceDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AlertReportService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "type", "status");

    private final AlertRepository alertRepository;
    private final PaginationService paginationService;

    public AlertReportService(AlertRepository alertRepository,
                              PaginationService paginationService) {
        this.alertRepository = alertRepository;
        this.paginationService = paginationService;
    }

    public List<AlertResponseDTO> getAlertReport(AlertReportFilterDTO filter) {
        Specification<Alert> specification = buildSpecification(filter);

        return alertRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<AlertResponseDTO> getAlertReportPage(AlertReportFilterDTO filter,
                                                     int page,
                                                     int size,
                                                     String sort,
                                                     String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        return alertRepository.findAll(buildSpecification(filter), pageable)
                .map(this::mapToResponseDTO);
    }

    public long countAlertReport(AlertReportFilterDTO filter) {
        return alertRepository.count(buildSpecification(filter));
    }

    public List<AlertTrendPointDTO> getAlertTrendReport(AlertReportFilterDTO filter) {
        List<Alert> alerts = alertRepository.findAll(
                buildSpecification(filter),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        Map<LocalDate, AlertCounterAccumulator> groupedByDate = new TreeMap<>();

        for (Alert alert : alerts) {
            if (alert.getCreatedAt() == null) {
                continue;
            }

            LocalDate date = alert.getCreatedAt().toLocalDate();
            AlertCounterAccumulator accumulator = groupedByDate.computeIfAbsent(date, ignored -> new AlertCounterAccumulator());
            accumulator.increment(alert);
        }

        return groupedByDate.entrySet()
                .stream()
                .map(entry -> new AlertTrendPointDTO(
                        entry.getKey(),
                        entry.getValue().totalAlerts,
                        entry.getValue().pendingAlerts,
                        entry.getValue().resolvedAlerts,
                        entry.getValue().discardedAlerts
                ))
                .toList();
    }

    public List<AlertTypeRecurrenceDTO> getAlertTypeRecurrenceReport(AlertReportFilterDTO filter) {
        List<Alert> alerts = alertRepository.findAll(
                buildSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Map<String, AlertTypeAccumulator> groupedByType = new LinkedHashMap<>();

        for (Alert alert : alerts) {
            if (alert.getType() == null) {
                continue;
            }

            String type = alert.getType().name();
            AlertTypeAccumulator accumulator = groupedByType.computeIfAbsent(type, ignored -> new AlertTypeAccumulator(type));
            accumulator.increment(alert);
        }

        return groupedByType.values()
                .stream()
                .sorted(
                        Comparator.comparingLong(AlertTypeAccumulator::getTotalAlerts).reversed()
                                .thenComparing(
                                        AlertTypeAccumulator::getLastAlertAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                )
                .map(acc -> new AlertTypeRecurrenceDTO(
                        acc.type,
                        acc.totalAlerts,
                        acc.pendingAlerts,
                        acc.resolvedAlerts,
                        acc.discardedAlerts,
                        acc.lastAlertAt
                ))
                .toList();
    }

    private Specification<Alert> buildSpecification(AlertReportFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getFrom()));
                }

                if (filter.getTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getTo()));
                }

                if (filter.getType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("type"), filter.getType()));
                }

                if (filter.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AlertResponseDTO mapToResponseDTO(Alert alert) {
        Long locationId = alert.getLocation() != null ? alert.getLocation().getId() : null;

        return new AlertResponseDTO(
                alert.getId(),
                alert.getType().name(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getStatus().name(),
                alert.getObservations(),
                alert.getCow().getId(),
                alert.getCow().getToken(),
                alert.getCow().getName(),
                locationId
        );
    }

    private static class AlertCounterAccumulator {
        protected long totalAlerts;
        protected long pendingAlerts;
        protected long resolvedAlerts;
        protected long discardedAlerts;

        protected void increment(Alert alert) {
            totalAlerts++;

            if (alert.getStatus() == AlertStatus.PENDIENTE) {
                pendingAlerts++;
            } else if (alert.getStatus() == AlertStatus.RESUELTA) {
                resolvedAlerts++;
            } else if (alert.getStatus() == AlertStatus.DESCARTADA) {
                discardedAlerts++;
            }
        }
    }

    private static class AlertTypeAccumulator extends AlertCounterAccumulator {
        private final String type;
        private LocalDateTime lastAlertAt;

        private AlertTypeAccumulator(String type) {
            this.type = type;
        }

        @Override
        protected void increment(Alert alert) {
            super.increment(alert);

            LocalDateTime createdAt = alert.getCreatedAt();
            if (createdAt != null && (lastAlertAt == null || createdAt.isAfter(lastAlertAt))) {
                lastAlertAt = createdAt;
            }
        }

        public long getTotalAlerts() {
            return totalAlerts;
        }

        public LocalDateTime getLastAlertAt() {
            return lastAlertAt;
        }
    }
}
