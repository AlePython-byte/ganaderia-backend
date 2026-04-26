package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CowIncidentAggregateView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

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
        return buildCowIncidentReport(filter, limit, SortMode.BY_TOTAL_INCIDENTS);
    }

    public List<CowIncidentReportDTO> getCowIncidentRecurrenceReport(AlertReportFilterDTO filter, Integer limit) {
        return buildCowIncidentReport(filter, limit, SortMode.BY_OPERATIONAL_RECURRENCE);
    }

    private List<CowIncidentReportDTO> buildCowIncidentReport(AlertReportFilterDTO filter,
                                                              Integer limit,
                                                              SortMode sortMode) {
        int effectiveLimit = paginationService.validateLimit(limit, 10);
        PageRequest topLimit = PageRequest.of(0, effectiveLimit);
        List<CowIncidentAggregateView> aggregates = sortMode == SortMode.BY_OPERATIONAL_RECURRENCE
                ? alertRepository.findCowIncidentAggregatesByOperationalRecurrence(
                extractFrom(filter),
                extractTo(filter),
                extractType(filter),
                extractStatus(filter),
                topLimit
        )
                : alertRepository.findCowIncidentAggregatesByTotalIncidents(
                extractFrom(filter),
                extractTo(filter),
                extractType(filter),
                extractStatus(filter),
                topLimit
        );

        LinkedHashMap<Long, String> lastIncidentTypeByCowId = resolveLastIncidentTypeByCowId(aggregates, filter);

        return aggregates.stream()
                .map(aggregate -> new CowIncidentReportDTO(
                        aggregate.getCowId(),
                        aggregate.getCowToken(),
                        aggregate.getCowName(),
                        aggregate.getTotalIncidents(),
                        aggregate.getPendingIncidents(),
                        aggregate.getResolvedIncidents(),
                        aggregate.getDiscardedIncidents(),
                        aggregate.getFirstIncidentAt(),
                        aggregate.getLastIncidentAt(),
                        aggregate.getCowStatus() != null ? aggregate.getCowStatus().name() : null,
                        lastIncidentTypeByCowId.get(aggregate.getCowId())
                ))
                .toList();
    }

    private LinkedHashMap<Long, String> resolveLastIncidentTypeByCowId(List<CowIncidentAggregateView> aggregates,
                                                                        AlertReportFilterDTO filter) {
        LinkedHashMap<Long, String> lastIncidentTypeByCowId = new LinkedHashMap<>();
        if (aggregates.isEmpty()) {
            return lastIncidentTypeByCowId;
        }

        List<Long> cowIds = aggregates.stream()
                .map(CowIncidentAggregateView::getCowId)
                .toList();

        for (Object[] row : alertRepository.findLatestIncidentTypesByCowIds(
                cowIds,
                extractFrom(filter),
                extractTo(filter),
                extractType(filter),
                extractStatus(filter)
        )) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }

            Long cowId = ((Number) row[0]).longValue();
            if (lastIncidentTypeByCowId.containsKey(cowId)) {
                continue;
            }

            AlertType lastIncidentType = (AlertType) row[1];
            lastIncidentTypeByCowId.put(cowId, lastIncidentType.name());
        }

        return lastIncidentTypeByCowId;
    }

    private LocalDateTime extractFrom(AlertReportFilterDTO filter) {
        return filter != null ? filter.getFrom() : null;
    }

    private LocalDateTime extractTo(AlertReportFilterDTO filter) {
        return filter != null ? filter.getTo() : null;
    }

    private AlertType extractType(AlertReportFilterDTO filter) {
        return filter != null ? filter.getType() : null;
    }

    private com.ganaderia4.backend.model.AlertStatus extractStatus(AlertReportFilterDTO filter) {
        return filter != null ? filter.getStatus() : null;
    }

    private enum SortMode {
        BY_TOTAL_INCIDENTS,
        BY_OPERATIONAL_RECURRENCE
    }
}
