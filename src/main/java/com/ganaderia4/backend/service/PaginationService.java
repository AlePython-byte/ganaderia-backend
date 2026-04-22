package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PaginationService {

    private final PaginationProperties paginationProperties;

    public PaginationService(PaginationProperties paginationProperties) {
        this.paginationProperties = paginationProperties;
    }

    public PageRequest createPageRequest(int page,
                                         int size,
                                         String sort,
                                         String direction,
                                         Set<String> allowedSortFields) {
        validatePageRequest(page, size);
        validateSort(sort, allowedSortFields);

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }

    public void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("El numero de pagina no puede ser negativo");
        }

        if (size < 1 || size > paginationProperties.getMaxSize()) {
            throw new BadRequestException("El tamano de pagina debe estar entre 1 y " + paginationProperties.getMaxSize());
        }
    }

    public int validateLimit(Integer limit, int defaultLimit) {
        int effectiveLimit = limit != null ? limit : defaultLimit;

        if (effectiveLimit < 1 || effectiveLimit > paginationProperties.getMaxSize()) {
            throw new BadRequestException("El limite debe estar entre 1 y " + paginationProperties.getMaxSize());
        }

        return effectiveLimit;
    }

    private void validateSort(String sort, Set<String> allowedSortFields) {
        if (sort == null || !allowedSortFields.contains(sort)) {
            throw new BadRequestException("Campo de ordenamiento no permitido");
        }
    }
}
