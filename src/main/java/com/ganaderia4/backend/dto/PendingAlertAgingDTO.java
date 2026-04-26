package com.ganaderia4.backend.dto;

public class PendingAlertAgingDTO {

    private long pendingAlerts;
    private long olderThan15Minutes;
    private long olderThan1Hour;
    private long olderThan6Hours;

    public PendingAlertAgingDTO() {
    }

    public PendingAlertAgingDTO(long pendingAlerts,
                                long olderThan15Minutes,
                                long olderThan1Hour,
                                long olderThan6Hours) {
        this.pendingAlerts = pendingAlerts;
        this.olderThan15Minutes = olderThan15Minutes;
        this.olderThan1Hour = olderThan1Hour;
        this.olderThan6Hours = olderThan6Hours;
    }

    public long getPendingAlerts() {
        return pendingAlerts;
    }

    public void setPendingAlerts(long pendingAlerts) {
        this.pendingAlerts = pendingAlerts;
    }

    public long getOlderThan15Minutes() {
        return olderThan15Minutes;
    }

    public void setOlderThan15Minutes(long olderThan15Minutes) {
        this.olderThan15Minutes = olderThan15Minutes;
    }

    public long getOlderThan1Hour() {
        return olderThan1Hour;
    }

    public void setOlderThan1Hour(long olderThan1Hour) {
        this.olderThan1Hour = olderThan1Hour;
    }

    public long getOlderThan6Hours() {
        return olderThan6Hours;
    }

    public void setOlderThan6Hours(long olderThan6Hours) {
        this.olderThan6Hours = olderThan6Hours;
    }
}
