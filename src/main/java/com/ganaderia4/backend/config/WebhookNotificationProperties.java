package com.ganaderia4.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.notifications.webhook")
public class WebhookNotificationProperties {

    private boolean enabled = false;
    private String url = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(5);
    private String secret = "";
    private boolean processorEnabled = true;
    private Duration processorFixedDelay = Duration.ofSeconds(15);
    private int processorBatchSize = 20;
    private int maxAttempts = 3;
    private Duration retryBackoff = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

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

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }
}
