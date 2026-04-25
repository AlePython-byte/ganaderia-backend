package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationServiceTest {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createdAt");

    private final PaginationService paginationService = new PaginationService(new PaginationProperties());

    @Test
    void shouldAcceptDirectionCaseInsensitively() {
        PageRequest pageRequest = paginationService.createPageRequest(0, 20, "id", "asc", ALLOWED_SORT_FIELDS);

        assertEquals(Sort.Direction.ASC, pageRequest.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void shouldRejectInvalidDirection() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> paginationService.createPageRequest(0, 20, "id", "DOWN", ALLOWED_SORT_FIELDS)
        );

        assertEquals("La direccion de ordenamiento debe ser ASC o DESC", exception.getMessage());
    }
}
