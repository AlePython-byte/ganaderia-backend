package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
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

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "type", "status");

    private final AlertRepository alertRepository;

    public AlertReportService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
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
        validatePageRequest(page, size, sort);

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return alertRepository.findAll(buildSpecification(filter), pageable)
                .map(this::mapToResponseDTO);
    }

    public long countAlertReport(AlertReportFilterDTO filter) {
        return alertRepository.count(buildSpecification(filter));
    }

    private void validatePageRequest(int page, int size, String sort) {
        if (page < 0) {
            throw new BadRequestException("El numero de pagina no puede ser negativo");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("El tamano de pagina debe estar entre 1 y " + MAX_PAGE_SIZE);
        }

        if (sort == null || !ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new BadRequestException("Campo de ordenamiento no permitido");
        }
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
}
