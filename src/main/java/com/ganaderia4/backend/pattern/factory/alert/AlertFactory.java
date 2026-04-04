package com.ganaderia4.backend.pattern.factory.alert;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlertFactory {

    private final Map<AlertType, AlertCreator> creators;

    public AlertFactory(List<AlertCreator> creators) {
        this.creators = creators.stream()
                .collect(Collectors.toMap(AlertCreator::getSupportedType, Function.identity()));
    }

    public Alert createAlert(AlertType type, Cow cow, Location location) {
        AlertCreator creator = creators.get(type);

        if (creator == null) {
            throw new IllegalArgumentException("No existe un creador para el tipo de alerta: " + type);
        }

        return creator.create(cow, location);
    }
}