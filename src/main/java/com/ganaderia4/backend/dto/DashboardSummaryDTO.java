package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class DashboardSummaryDTO {

    private long totalCows;
    private long cowsOutsideGeofence;
    private long totalCollars;
    private long activeCollars;
    private long offlineCollars;
    private long pendingAlerts;
    private long pendingExitGeofenceAlerts;
    private long pendingCollarOfflineAlerts;
    private LocalDateTime latestLocationTimestamp;

    public DashboardSummaryDTO() {
    }

    public DashboardSummaryDTO(long totalCows,
                               long cowsOutsideGeofence,
                               long totalCollars,
                               long activeCollars,
                               long offlineCollars,
                               long pendingAlerts,
                               long pendingExitGeofenceAlerts,
                               long pendingCollarOfflineAlerts,
                               LocalDateTime latestLocationTimestamp) {
        this.totalCows = totalCows;
        this.cowsOutsideGeofence = cowsOutsideGeofence;
        this.totalCollars = totalCollars;
        this.activeCollars = activeCollars;
        this.offlineCollars = offlineCollars;
        this.pendingAlerts = pendingAlerts;
        this.pendingExitGeofenceAlerts = pendingExitGeofenceAlerts;
        this.pendingCollarOfflineAlerts = pendingCollarOfflineAlerts;
        this.latestLocationTimestamp = latestLocationTimestamp;
    }

    public long getTotalCows() {
        return totalCows;
    }

    public void setTotalCows(long totalCows) {
        this.totalCows = totalCows;
    }

    public long getCowsOutsideGeofence() {
        return cowsOutsideGeofence;
    }

    public void setCowsOutsideGeofence(long cowsOutsideGeofence) {
        this.cowsOutsideGeofence = cowsOutsideGeofence;
    }

    public long getTotalCollars() {
        return totalCollars;
    }

    public void setTotalCollars(long totalCollars) {
        this.totalCollars = totalCollars;
    }

    public long getActiveCollars() {
        return activeCollars;
    }

    public void setActiveCollars(long activeCollars) {
        this.activeCollars = activeCollars;
    }

    public long getOfflineCollars() {
        return offlineCollars;
    }

    public void setOfflineCollars(long offlineCollars) {
        this.offlineCollars = offlineCollars;
    }

    public long getPendingAlerts() {
        return pendingAlerts;
    }

    public void setPendingAlerts(long pendingAlerts) {
        this.pendingAlerts = pendingAlerts;
    }

    public long getPendingExitGeofenceAlerts() {
        return pendingExitGeofenceAlerts;
    }

    public void setPendingExitGeofenceAlerts(long pendingExitGeofenceAlerts) {
        this.pendingExitGeofenceAlerts = pendingExitGeofenceAlerts;
    }

    public long getPendingCollarOfflineAlerts() {
        return pendingCollarOfflineAlerts;
    }

    public void setPendingCollarOfflineAlerts(long pendingCollarOfflineAlerts) {
        this.pendingCollarOfflineAlerts = pendingCollarOfflineAlerts;
    }

    public LocalDateTime getLatestLocationTimestamp() {
        return latestLocationTimestamp;
    }

    public void setLatestLocationTimestamp(LocalDateTime latestLocationTimestamp) {
        this.latestLocationTimestamp = latestLocationTimestamp;
    }
}