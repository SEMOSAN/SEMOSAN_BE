package com.semosan.api.common.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "discord.alert")
public class DiscordAlertProperties {

    private boolean enabled;
    private String webhookUrl;
    private String reportWebhookUrl;
    private String signupWebhookUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getReportWebhookUrl() {
        return reportWebhookUrl;
    }

    public void setReportWebhookUrl(String reportWebhookUrl) {
        this.reportWebhookUrl = reportWebhookUrl;
    }

    public String getSignupWebhookUrl() {
        return signupWebhookUrl;
    }

    public void setSignupWebhookUrl(String signupWebhookUrl) {
        this.signupWebhookUrl = signupWebhookUrl;
    }
}
