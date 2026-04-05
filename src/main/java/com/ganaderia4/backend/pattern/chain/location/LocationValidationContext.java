package com.ganaderia4.backend.pattern.chain.location;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;

public class LocationValidationContext {

    private final LocationCommand command;
    private Collar collar;
    private Cow cow;

    public LocationValidationContext(LocationCommand command) {
        this.command = command;
    }

    public LocationCommand getCommand() {
        return command;
    }

    public Collar getCollar() {
        return collar;
    }

    public void setCollar(Collar collar) {
        this.collar = collar;
    }

    public Cow getCow() {
        return cow;
    }

    public void setCow(Cow cow) {
        this.cow = cow;
    }
}