package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CoordinateValidationHandler implements LocationValidationHandler {

    @Override
    public void handle(LocationValidationContext context) {
        Double latitude = context.getCommand().getLatitude();
        Double longitude = context.getCommand().getLongitude();

        if (latitude == null || longitude == null) {
            throw new BadRequestException("Las coordenadas son obligatorias");
        }

        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Latitud no válida");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Longitud no válida");
        }
    }
}