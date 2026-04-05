package com.ganaderia4.backend.pattern.chain.location;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocationValidationChain {

    private final List<LocationValidationHandler> handlers;

    public LocationValidationChain(List<LocationValidationHandler> handlers) {
        this.handlers = handlers;
    }

    public void validate(LocationValidationContext context) {
        for (LocationValidationHandler handler : handlers) {
            handler.handle(context);
        }
    }
}