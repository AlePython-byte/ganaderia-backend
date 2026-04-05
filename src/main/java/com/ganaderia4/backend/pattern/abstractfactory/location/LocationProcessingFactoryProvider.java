package com.ganaderia4.backend.pattern.abstractfactory.location;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LocationProcessingFactoryProvider {

    private final Map<String, LocationProcessingFactory<?>> factories;

    public LocationProcessingFactoryProvider(List<LocationProcessingFactory<?>> factories) {
        this.factories = factories.stream()
                .collect(Collectors.toMap(
                        factory -> factory.getSourceType().toUpperCase(),
                        Function.identity()
                ));
    }

    @SuppressWarnings("unchecked")
    public <T> LocationProcessingFactory<T> getFactory(String sourceType) {
        LocationProcessingFactory<?> factory = factories.get(sourceType.toUpperCase());

        if (factory == null) {
            throw new IllegalArgumentException("No existe una fábrica para el tipo de origen: " + sourceType);
        }

        return (LocationProcessingFactory<T>) factory;
    }
}