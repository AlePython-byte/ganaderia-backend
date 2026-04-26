package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertTrendPointDTO;
import com.ganaderia4.backend.dto.AlertTypeRecurrenceDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        return alertRepository.findTrendAggregates(
                        extractFrom(filter),
                        extractTo(filter),
                        extractType(filter),
                        extractStatus(filter)
                )
                .stream()
                .map(aggregate -> new AlertTrendPointDTO(
                        aggregate.getDate(),
                        aggregate.getTotalAlerts(),
                        aggregate.getPendingAlerts(),
                        aggregate.getResolvedAlerts(),
                        aggregate.getDiscardedAlerts()
                ))
                .toList();
    }

    public List<AlertTypeRecurrenceDTO> getAlertTypeRecurrenceReport(AlertReportFilterDTO filter) {
        return alertRepository.findTypeRecurrenceAggregates(
                        extractFrom(filter),
                        extractTo(filter),
                        extractType(filter),
                        extractStatus(filter)
                )
                .stream()
                .map(aggregate -> new AlertTypeRecurrenceDTO(
                        aggregate.getType(),
                        aggregate.getTotalAlerts(),
                        aggregate.getPendingAlerts(),
                        aggregate.getResolvedAlerts(),
                        aggregate.getDiscardedAlerts(),
                        aggregate.getLastAlertAt()
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

    private java.time.LocalDateTime extractFrom(AlertReportFilterDTO filter) {
        return filter != null ? filter.getFrom() : null;
    }

    private java.time.LocalDateTime extractTo(AlertReportFilterDTO filter) {
        return filter != null ? filter.getTo() : null;
    }

    private String extractType(AlertReportFilterDTO filter) {
        return filter != null && filter.getType() != null ? filter.getType().name() : null;
    }

    private String extractStatus(AlertReportFilterDTO filter) {
        return filter != null && filter.getStatus() != null ? filter.getStatus().name() : null;
    }
}
