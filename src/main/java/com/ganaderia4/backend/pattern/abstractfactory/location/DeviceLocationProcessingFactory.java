package com.ganaderia4.backend.pattern.abstractfactory.location;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.pattern.adapter.location.DeviceLocationPayloadAdapter;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import org.springframework.stereotype.Component;

@Component
public class DeviceLocationProcessingFactory implements LocationProcessingFactory<DeviceLocationPayloadDTO> {

    private final DeviceLocationPayloadAdapter deviceLocationPayloadAdapter;
    private final LocationValidationChain locationValidationChain;

    public DeviceLocationProcessingFactory(DeviceLocationPayloadAdapter deviceLocationPayloadAdapter,
                                           LocationValidationChain locationValidationChain) {
        this.deviceLocationPayloadAdapter = deviceLocationPayloadAdapter;
        this.locationValidationChain = locationValidationChain;
    }

    @Override
    public String getSourceType() {
        return "DEVICE";
    }

    @Override
    public LocationCommand createCommand(DeviceLocationPayloadDTO input) {
        return deviceLocationPayloadAdapter.adapt(input);
    }

    @Override
    public LocationValidationChain getValidationChain() {
        return locationValidationChain;
    }
}