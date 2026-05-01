package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.GpsAccuracyQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpsAccuracyClassifierTest {

    private final GpsAccuracyClassifier classifier = new GpsAccuracyClassifier();

    @Test
    void shouldClassifyNullAccuracyAsUnknown() {
        assertEquals(GpsAccuracyQuality.UNKNOWN, classifier.classify(null));
    }

    @Test
    void shouldClassifyZeroAccuracyAsGood() {
        assertEquals(GpsAccuracyQuality.GOOD, classifier.classify(0.0));
    }

    @Test
    void shouldClassifyTenMetersAsGood() {
        assertEquals(GpsAccuracyQuality.GOOD, classifier.classify(10.0));
    }

    @Test
    void shouldClassifyJustAboveTenMetersAsModerate() {
        assertEquals(GpsAccuracyQuality.MODERATE, classifier.classify(10.1));
    }

    @Test
    void shouldClassifyThirtyMetersAsModerate() {
        assertEquals(GpsAccuracyQuality.MODERATE, classifier.classify(30.0));
    }

    @Test
    void shouldClassifyJustAboveThirtyMetersAsLow() {
        assertEquals(GpsAccuracyQuality.LOW, classifier.classify(30.1));
    }
}
