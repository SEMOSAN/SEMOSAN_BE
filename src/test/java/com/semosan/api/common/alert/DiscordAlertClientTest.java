package com.semosan.api.common.alert;

import com.semosan.api.common.alert.dto.DiscordEmbed;
import com.semosan.api.common.alert.dto.DiscordMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;

class DiscordAlertClientTest {

    @Test
    void sendDoesNothingWhenDisabled() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(false, "https://discord.example.com/webhook", null);
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.send(message());

        assertThat(calls).hasValue(0);
    }

    @Test
    void sendDoesNothingWhenWebhookUrlIsBlank() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, " ", null);
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.send(message());

        assertThat(calls).hasValue(0);
    }

    @Test
    void sendPostsToConfiguredWebhook() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, "https://discord.example.com/webhook", null);
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.send(message());

        assertThat(calls).hasValue(1);
    }

    @Test
    void sendReportPostsToReportWebhook() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, null, "https://discord.example.com/report");
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.sendReport(message());

        assertThat(calls).hasValue(1);
    }

    @Test
    void sendSignupPostsToSignupWebhook() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, null, null);
        properties.setSignupWebhookUrl("https://discord.example.com/signup");
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.sendSignup(message());

        assertThat(calls).hasValue(1);
    }

    @Test
    void sendSignupDoesNothingWhenSignupWebhookUrlIsMissing() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, "https://discord.example.com/webhook", null);
        DiscordAlertClient client = new DiscordAlertClient(properties, webClientBuilder(calls));

        client.sendSignup(message());

        assertThat(calls).hasValue(0);
    }

    @Test
    void sendSwallowsClientFailure() {
        AtomicInteger calls = new AtomicInteger();
        DiscordAlertProperties properties = properties(true, "https://discord.example.com/webhook", null);
        DiscordAlertClient client = new DiscordAlertClient(properties, WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.error(new RuntimeException("network"));
                }));

        client.send(message());

        assertThat(calls).hasValue(1);
    }

    private WebClient.Builder webClientBuilder(AtomicInteger calls) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.just(ClientResponse.create(NO_CONTENT).build());
                });
    }

    private DiscordAlertProperties properties(boolean enabled, String webhookUrl, String reportWebhookUrl) {
        DiscordAlertProperties properties = new DiscordAlertProperties();
        properties.setEnabled(enabled);
        properties.setWebhookUrl(webhookUrl);
        properties.setReportWebhookUrl(reportWebhookUrl);
        return properties;
    }

    private DiscordMessage message() {
        return new DiscordMessage("content", List.of(new DiscordEmbed("title", "description", 1)));
    }
}
