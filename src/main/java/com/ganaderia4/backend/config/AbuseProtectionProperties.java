package com.ganaderia4.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.abuse-protection")
public class AbuseProtectionProperties {

    private boolean enabled = true;
    private ClientIp clientIp = new ClientIp();
    private Login login = new Login();
    private Device device = new Device();
    private PasswordReset passwordReset = new PasswordReset();
    private AiSummary aiSummary = new AiSummary();
    private OutboxRequeue outboxRequeue = new OutboxRequeue();
    private Email email = new Email();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ClientIp getClientIp() {
        return clientIp;
    }

    public void setClientIp(ClientIp clientIp) {
        this.clientIp = clientIp;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    public void setPasswordReset(PasswordReset passwordReset) {
        this.passwordReset = passwordReset;
    }

    public AiSummary getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(AiSummary aiSummary) {
        this.aiSummary = aiSummary;
    }

    public OutboxRequeue getOutboxRequeue() {
        return outboxRequeue;
    }

    public void setOutboxRequeue(OutboxRequeue outboxRequeue) {
        this.outboxRequeue = outboxRequeue;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public static class ClientIp {
        private boolean trustForwardedHeaders = false;

        public boolean isTrustForwardedHeaders() {
            return trustForwardedHeaders;
        }

        public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
            this.trustForwardedHeaders = trustForwardedHeaders;
        }
    }

    public static class Login {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(15);
        private int maxAttempts = 5;
        private Duration blockDuration = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class Device {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(1);
        private int maxAttempts = 300;
        private Duration blockDuration = Duration.ofMinutes(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class PasswordReset {
        private Forgot forgot = new Forgot();
        private Reset reset = new Reset();

        public Forgot getForgot() {
            return forgot;
        }

        public void setForgot(Forgot forgot) {
            this.forgot = forgot;
        }

        public Reset getReset() {
            return reset;
        }

        public void setReset(Reset reset) {
            this.reset = reset;
        }
    }

    public static class Forgot {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(15);
        private int maxAttempts = 3;
        private Duration blockDuration = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class Reset {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(15);
        private int maxAttempts = 5;
        private Duration blockDuration = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class AiSummary {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(10);
        private int maxAttempts = 10;
        private Duration blockDuration = Duration.ofMinutes(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class OutboxRequeue {
        private boolean enabled = true;
        private Duration window = Duration.ofMinutes(10);
        private int maxAttempts = 5;
        private Duration blockDuration = Duration.ofMinutes(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }

    public static class Email {
        private boolean enabled = true;
        private EmailLimit recipient = new EmailLimit(10, Duration.ofMinutes(10), Duration.ofMinutes(10));
        private EmailLimit recipientEventType = new EmailLimit(5, Duration.ofMinutes(10), Duration.ofMinutes(10));
        private EmailLimit channel = new EmailLimit(100, Duration.ofMinutes(10), Duration.ofMinutes(5));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public EmailLimit getRecipient() {
            return recipient;
        }

        public void setRecipient(EmailLimit recipient) {
            this.recipient = recipient;
        }

        public EmailLimit getRecipientEventType() {
            return recipientEventType;
        }

        public void setRecipientEventType(EmailLimit recipientEventType) {
            this.recipientEventType = recipientEventType;
        }

        public EmailLimit getChannel() {
            return channel;
        }

        public void setChannel(EmailLimit channel) {
            this.channel = channel;
        }
    }

    public static class EmailLimit {
        private Duration window;
        private int maxAttempts;
        private Duration blockDuration;

        public EmailLimit() {
            this(10, Duration.ofMinutes(10), Duration.ofMinutes(10));
        }

        public EmailLimit(int maxAttempts, Duration window, Duration blockDuration) {
            this.maxAttempts = maxAttempts;
            this.window = window;
            this.blockDuration = blockDuration;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBlockDuration() {
            return blockDuration;
        }

        public void setBlockDuration(Duration blockDuration) {
            this.blockDuration = blockDuration;
        }
    }
}
