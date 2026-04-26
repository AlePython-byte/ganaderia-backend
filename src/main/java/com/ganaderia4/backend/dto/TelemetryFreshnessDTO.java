package com.ganaderia4.backend.dto;

public class TelemetryFreshnessDTO {

    private long enabledCollars;
    private long neverReported;
    private long reportingWithinThreshold;
    private long lastSeenOlderThanThreshold;
    private long lastSeenOlderThan1Hour;
    private long lastSeenOlderThan6Hours;
    private long operationalThresholdMinutes;

    public TelemetryFreshnessDTO() {
    }

    public TelemetryFreshnessDTO(long enabledCollars,
                                 long neverReported,
                                 long reportingWithinThreshold,
                                 long lastSeenOlderThanThreshold,
                                 long lastSeenOlderThan1Hour,
                                 long lastSeenOlderThan6Hours,
                                 long operationalThresholdMinutes) {
        this.enabledCollars = enabledCollars;
        this.neverReported = neverReported;
        this.reportingWithinThreshold = reportingWithinThreshold;
        this.lastSeenOlderThanThreshold = lastSeenOlderThanThreshold;
        this.lastSeenOlderThan1Hour = lastSeenOlderThan1Hour;
        this.lastSeenOlderThan6Hours = lastSeenOlderThan6Hours;
        this.operationalThresholdMinutes = operationalThresholdMinutes;
    }

    public long getEnabledCollars() {
        return enabledCollars;
    }

    public void setEnabledCollars(long enabledCollars) {
        this.enabledCollars = enabledCollars;
    }

    public long getNeverReported() {
        return neverReported;
    }

    public void setNeverReported(long neverReported) {
        this.neverReported = neverReported;
    }

    public long getReportingWithinThreshold() {
        return reportingWithinThreshold;
    }

    public void setReportingWithinThreshold(long reportingWithinThreshold) {
        this.reportingWithinThreshold = reportingWithinThreshold;
    }

    public long getLastSeenOlderThanThreshold() {
        return lastSeenOlderThanThreshold;
    }

    public void setLastSeenOlderThanThreshold(long lastSeenOlderThanThreshold) {
        this.lastSeenOlderThanThreshold = lastSeenOlderThanThreshold;
    }

    public long getLastSeenOlderThan1Hour() {
        return lastSeenOlderThan1Hour;
    }

    public void setLastSeenOlderThan1Hour(long lastSeenOlderThan1Hour) {
        this.lastSeenOlderThan1Hour = lastSeenOlderThan1Hour;
    }

    public long getLastSeenOlderThan6Hours() {
        return lastSeenOlderThan6Hours;
    }

    public void setLastSeenOlderThan6Hours(long lastSeenOlderThan6Hours) {
        this.lastSeenOlderThan6Hours = lastSeenOlderThan6Hours;
    }

    public long getOperationalThresholdMinutes() {
        return operationalThresholdMinutes;
    }

    public void setOperationalThresholdMinutes(long operationalThresholdMinutes) {
        this.operationalThresholdMinutes = operationalThresholdMinutes;
    }
}
