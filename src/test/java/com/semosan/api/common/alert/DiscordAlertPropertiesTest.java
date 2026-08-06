package com.semosan.api.common.alert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordAlertPropertiesTest {

    @Test
    void settersAndGettersExposeConfiguredValues() {
        DiscordAlertProperties properties = new DiscordAlertProperties();

        properties.setEnabled(true);
        properties.setWebhookUrl("https://discord.example.com/webhook");
        properties.setReportWebhookUrl("https://discord.example.com/report");

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getWebhookUrl()).isEqualTo("https://discord.example.com/webhook");
        assertThat(properties.getReportWebhookUrl()).isEqualTo("https://discord.example.com/report");
    }
}
