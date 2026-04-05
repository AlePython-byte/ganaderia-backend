package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class CollarAssignedValidationHandler implements LocationValidationHandler {

    @Override
    public void handle(LocationValidationContext context) {
        Collar collar = context.getCollar();

        if (collar == null) {
            throw new BadRequestException("No se pudo validar el collar");
        }

        if (collar.getCow() == null) {
            throw new BadRequestException("El collar no está asociado a ninguna vaca");
        }

        context.setCow(collar.getCow());
    }
}