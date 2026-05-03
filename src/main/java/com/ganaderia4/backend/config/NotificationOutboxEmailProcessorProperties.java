package com.ganaderia4.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.notifications.outbox.email")
public class NotificationOutboxEmailProcessorProperties {

    private boolean processorEnabled = false;
    private Duration processorFixedDelay = Duration.ofSeconds(30);
    private int processorBatchSize = 20;
    private Duration retryBackoff = Duration.ofMinutes(1);
    private Duration processingTimeout = Duration.ofMinutes(5);

    public boolean isProcessorEnabled() {
        return processorEnabled;
    }

    public void setProcessorEnabled(boolean processorEnabled) {
        this.processorEnabled = processorEnabled;
    }

    public Duration getProcessorFixedDelay() {
        return processorFixedDelay;
    }

    public void setProcessorFixedDelay(Duration processorFixedDelay) {
        this.processorFixedDelay = processorFixedDelay;
    }

    public int getProcessorBatchSize() {
        return processorBatchSize;
    }

    public void setProcessorBatchSize(int processorBatchSize) {
        this.processorBatchSize = processorBatchSize;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }
}
