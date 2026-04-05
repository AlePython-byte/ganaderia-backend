package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class CollarActiveValidationHandler implements LocationValidationHandler {

    @Override
    public void handle(LocationValidationContext context) {
        Collar collar = context.getCollar();

        if (collar == null) {
            throw new BadRequestException("No se pudo validar el estado del collar");
        }

        if (collar.getStatus() != CollarStatus.ACTIVO) {
            throw new BadRequestException("El collar no está activo");
        }
    }
}