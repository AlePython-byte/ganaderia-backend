package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class CollarEnabledValidationHandler implements LocationValidationHandler {

    @Override
    public void handle(LocationValidationContext context) {
        Collar collar = context.getCollar();

        if (collar == null) {
            throw new BadRequestException("No se pudo validar si el collar está habilitado");
        }

        if (!Boolean.TRUE.equals(collar.getEnabled())) {
            throw new BadRequestException("El collar está deshabilitado");
        }
    }
}
