package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class CollarExistsValidationHandler implements LocationValidationHandler {

    private final CollarRepository collarRepository;

    public CollarExistsValidationHandler(CollarRepository collarRepository) {
        this.collarRepository = collarRepository;
    }

    @Override
    public void handle(LocationValidationContext context) {
        String collarToken = context.getCommand().getCollarToken();

        if (collarToken == null || collarToken.isBlank()) {
            throw new BadRequestException("El token del collar es obligatorio");
        }

        Collar collar = collarRepository.findByToken(collarToken)
                .orElseThrow(() -> new ResourceNotFoundException("Collar no registrado"));

        context.setCollar(collar);
    }
}