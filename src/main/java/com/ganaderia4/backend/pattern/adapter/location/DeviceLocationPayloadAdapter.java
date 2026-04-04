package com.ganaderia4.backend.pattern.adapter.location;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import org.springframework.stereotype.Component;

@Component
public class DeviceLocationPayloadAdapter implements LocationInputAdapter<DeviceLocationPayloadDTO> {

    @Override
    public LocationCommand adapt(DeviceLocationPayloadDTO input) {
        return new LocationCommand(
                input.getDeviceToken(),
                input.getLat(),
                input.getLon(),
                input.getReportedAt()
        );
    }
}