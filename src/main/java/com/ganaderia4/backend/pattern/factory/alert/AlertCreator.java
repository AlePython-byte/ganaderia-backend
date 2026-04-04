package com.ganaderia4.backend.pattern.factory.alert;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;

public interface AlertCreator {

    AlertType getSupportedType();

    Alert create(Cow cow, Location location);
}