package com.semosan.api.common.alert;

import com.semosan.api.common.alert.dto.DiscordMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
public class DiscordAlertClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final DiscordAlertProperties properties;
    private final WebClient webClient;

    public DiscordAlertClient(DiscordAlertProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    public void send(DiscordMessage message) {
        doSend(properties.getWebhookUrl(), message);
    }

    public void sendReport(DiscordMessage message) {
        doSend(properties.getReportWebhookUrl(), message);
    }

    private void doSend(String webhookUrl, DiscordMessage message) {
        if (!properties.isEnabled() || !StringUtils.hasText(webhookUrl)) {
            return;
        }

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(message)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.warn("[*] Discord alert send failed: {}", e.getMessage());
        }
    }
}
