package com.ganaderia4.backend.pattern.abstractfactory.location;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.pattern.adapter.location.ApiLocationRequestAdapter;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import org.springframework.stereotype.Component;

@Component
public class ApiLocationProcessingFactory implements LocationProcessingFactory<LocationRequestDTO> {

    private final ApiLocationRequestAdapter apiLocationRequestAdapter;
    private final LocationValidationChain locationValidationChain;

    public ApiLocationProcessingFactory(ApiLocationRequestAdapter apiLocationRequestAdapter,
                                        LocationValidationChain locationValidationChain) {
        this.apiLocationRequestAdapter = apiLocationRequestAdapter;
        this.locationValidationChain = locationValidationChain;
    }

    @Override
    public String getSourceType() {
        return "API";
    }

    @Override
    public LocationCommand createCommand(LocationRequestDTO input) {
        return apiLocationRequestAdapter.adapt(input);
    }

    @Override
    public LocationValidationChain getValidationChain() {
        return locationValidationChain;
    }
}