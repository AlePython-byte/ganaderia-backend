package com.ganaderia4.backend.service;

public record AlertPriorityAssessment(Integer priorityScore, String priority) {

    private static final AlertPriorityAssessment NONE = new AlertPriorityAssessment(null, null);

    public static AlertPriorityAssessment none() {
        return NONE;
    }
}
