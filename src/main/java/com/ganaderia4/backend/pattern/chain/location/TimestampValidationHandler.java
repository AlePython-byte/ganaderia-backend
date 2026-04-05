package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
public class TimestampValidationHandler implements LocationValidationHandler {

    @Override
    public void handle(LocationValidationContext context) {
        if (context.getCommand().getTimestamp() == null) {
            throw new BadRequestException("La fecha y hora son obligatorias");
        }

        if (context.getCommand().getTimestamp().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("La fecha y hora no pueden estar en el futuro");
        }
    }
}