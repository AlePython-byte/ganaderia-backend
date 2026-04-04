package com.ganaderia4.backend.pattern.adapter.location;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ApiLocationRequestAdapter implements LocationInputAdapter<LocationRequestDTO> {

    @Override
    public LocationCommand adapt(LocationRequestDTO input) {
        return new LocationCommand(
                input.getCollarToken(),
                input.getLatitude(),
                input.getLongitude(),
                input.getTimestamp()
        );
    }
}