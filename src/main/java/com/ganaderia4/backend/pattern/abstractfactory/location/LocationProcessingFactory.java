package com.ganaderia4.backend.pattern.abstractfactory.location;

import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;

public interface LocationProcessingFactory<T> {

    String getSourceType();

    LocationCommand createCommand(T input);

    LocationValidationChain getValidationChain();
}