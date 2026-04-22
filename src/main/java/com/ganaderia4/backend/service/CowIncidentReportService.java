package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CowIncidentReportService {

    private final AlertRepository alertRepository;
    private final PaginationService paginationService;

    public CowIncidentReportService(AlertRepository alertRepository,
                                    PaginationService paginationService) {
        this.alertRepository = alertRepository;
        this.paginationService = paginationService;
    }

    public List<CowIncidentReportDTO> getCowsMostIncidentsReport(AlertReportFilterDTO filter, Integer limit) {
        int effectiveLimit = paginationService.validateLimit(limit, 10);

        List<Alert> alerts = alertRepository.findAll(
                buildSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Map<Long, CowIncidentAccumulator> grouped = new LinkedHashMap<>();

        for (Alert alert : alerts) {
            if (alert.getCow() == null || alert.getCow().getId() == null) {
                continue;
            }

            Long cowId = alert.getCow().getId();

            CowIncidentAccumulator accumulator = grouped.computeIfAbsent(
                    cowId,
                    ignored -> new CowIncidentAccumulator(
                            cowId,
                            alert.getCow().getToken(),
                            alert.getCow().getName()
                    )
            );

            accumulator.totalIncidents++;

            if (alert.getStatus() == AlertStatus.PENDIENTE) {
                accumulator.pendingIncidents++;
            } else if (alert.getStatus() == AlertStatus.RESUELTA) {
                accumulator.resolvedIncidents++;
            } else if (alert.getStatus() == AlertStatus.DESCARTADA) {
                accumulator.discardedIncidents++;
            }

            LocalDateTime createdAt = alert.getCreatedAt();
            if (createdAt != null && (accumulator.lastIncidentAt == null || createdAt.isAfter(accumulator.lastIncidentAt))) {
                accumulator.lastIncidentAt = createdAt;
            }
        }

        return grouped.values()
                .stream()
                .sorted(
                        Comparator.comparingLong(CowIncidentAccumulator::getTotalIncidents).reversed()
                                .thenComparing(
                                        CowIncidentAccumulator::getLastIncidentAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                )
                .limit(effectiveLimit)
                .map(acc -> new CowIncidentReportDTO(
                        acc.cowId,
                        acc.cowToken,
                        acc.cowName,
                        acc.totalIncidents,
                        acc.pendingIncidents,
                        acc.resolvedIncidents,
                        acc.discardedIncidents,
                        acc.lastIncidentAt
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

    private static class CowIncidentAccumulator {
        private final Long cowId;
        private final String cowToken;
        private final String cowName;
        private long totalIncidents;
        private long pendingIncidents;
        private long resolvedIncidents;
        private long discardedIncidents;
        private LocalDateTime lastIncidentAt;

        private CowIncidentAccumulator(Long cowId, String cowToken, String cowName) {
            this.cowId = cowId;
            this.cowToken = cowToken;
            this.cowName = cowName;
        }

        public long getTotalIncidents() {
            return totalIncidents;
        }

        public LocalDateTime getLastIncidentAt() {
            return lastIncidentAt;
        }
    }
}
