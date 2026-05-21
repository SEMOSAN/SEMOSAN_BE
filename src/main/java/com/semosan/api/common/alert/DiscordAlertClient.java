package com.semosan.api.common.alert;

import com.semosan.api.common.alert.dto.DiscordMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordAlertClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final DiscordAlertProperties properties;
    private final WebClient.Builder webClientBuilder;

    public void send(DiscordMessage message) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getWebhookUrl())) {
            return;
        }

        try {
            webClientBuilder.build()
                    .post()
                    .uri(properties.getWebhookUrl())
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
