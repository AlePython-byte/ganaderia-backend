package com.ganaderia4.backend.pattern.adapter.location;

public interface LocationInputAdapter<T> {

    LocationCommand adapt(T input);
}