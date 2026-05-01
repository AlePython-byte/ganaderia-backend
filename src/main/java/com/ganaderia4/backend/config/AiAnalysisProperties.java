package com.ganaderia4.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiAnalysisProperties {

    private boolean enabled = false;
    private String provider = "gemini";
    private String geminiApiKey = "";
    private String geminiModel = "gemini-2.5-flash";
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
    private Duration geminiConnectTimeout = Duration.ofSeconds(5);
    private Duration geminiReadTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public String getGeminiBaseUrl() {
        return geminiBaseUrl;
    }

    public void setGeminiBaseUrl(String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    public Duration getGeminiConnectTimeout() {
        return geminiConnectTimeout;
    }

    public void setGeminiConnectTimeout(Duration geminiConnectTimeout) {
        this.geminiConnectTimeout = geminiConnectTimeout;
    }

    public Duration getGeminiReadTimeout() {
        return geminiReadTimeout;
    }

    public void setGeminiReadTimeout(Duration geminiReadTimeout) {
        this.geminiReadTimeout = geminiReadTimeout;
    }
}
