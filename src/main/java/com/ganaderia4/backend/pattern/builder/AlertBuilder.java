package com.ganaderia4.backend.pattern.builder;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;

import java.time.LocalDateTime;

public class AlertBuilder {

    private Long id;
    private AlertType type;
    private String message;
    private LocalDateTime createdAt;
    private AlertStatus status;
    private String observations;
    private Cow cow;
    private Location location;

    public AlertBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public AlertBuilder type(AlertType type) {
        this.type = type;
        return this;
    }

    public AlertBuilder message(String message) {
        this.message = message;
        return this;
    }

    public AlertBuilder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public AlertBuilder status(AlertStatus status) {
        this.status = status;
        return this;
    }

    public AlertBuilder observations(String observations) {
        this.observations = observations;
        return this;
    }

    public AlertBuilder cow(Cow cow) {
        this.cow = cow;
        return this;
    }

    public AlertBuilder location(Location location) {
        this.location = location;
        return this;
    }

    public Alert build() {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setType(type);
        alert.setMessage(message);
        alert.setCreatedAt(createdAt);
        alert.setStatus(status);
        alert.setObservations(observations);
        alert.setCow(cow);
        alert.setLocation(location);
        return alert;
    }
}