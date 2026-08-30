package com.semosan.api.common.alert;

import com.semosan.api.common.alert.dto.DiscordMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerErrorAlertServiceTest {

    @Mock
    private DiscordAlertClient discordAlertClient;

    @Mock
    private Environment environment;

    @InjectMocks
    private ServerErrorAlertService alertService;

    @Test
    void notifyBuildsDiscordMessageWithRequestContextAndMasksSensitiveValues() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        RequestContext requestContext = new RequestContext(
                "GET",
                "https://api.example.com/fail",
                "127.0.0.1",
                "1",
                "JUnit token=secret",
                "trace-1"
        );
        IllegalStateException exception = new IllegalStateException("password=raw leaked");
        ArgumentCaptor<DiscordMessage> captor = ArgumentCaptor.forClass(DiscordMessage.class);

        alertService.notify(500, exception, requestContext);

        verify(discordAlertClient).send(captor.capture());
        DiscordMessage message = captor.getValue();
        assertThat(message.content()).contains("서버 에러 발생");
        assertThat(message.embeds()).singleElement()
                .satisfies(embed -> {
                    assertThat(embed.title()).isEqualTo("에러 정보");
                    assertThat(embed.description())
                            .contains("test")
                            .contains("[GET] https://api.example.com/fail")
                            .contains("[UserId]: 1")
                            .contains("password=***")
                            .contains("token=***")
                            .doesNotContain("password=raw")
                            .doesNotContain("token=secret");
                    assertThat(embed.color()).isEqualTo(0xED4245);
                });
    }

    @Test
    void notifyUsesDefaultProfilesWhenNoActiveProfileExists() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(environment.getDefaultProfiles()).thenReturn(new String[]{"default"});
        RequestContext requestContext = new RequestContext("POST", "https://api.example.com/fail", "127.0.0.1", null, null, "trace-2");
        ArgumentCaptor<DiscordMessage> captor = ArgumentCaptor.forClass(DiscordMessage.class);

        alertService.notify(500, new RuntimeException(), requestContext);

        verify(discordAlertClient).send(captor.capture());
        assertThat(captor.getValue().embeds().getFirst().description()).contains("default");
    }

    @Test
    void notifyIncludesGrafanaLogLinkWhenConfigured() {
        ReflectionTestUtils.setField(alertService, "grafanaBaseUrl", "http://grafana.example.com");
        ReflectionTestUtils.setField(alertService, "lokiDatasourceUid", "loki");
        ReflectionTestUtils.setField(alertService, "lokiNamespace", "test");
        ReflectionTestUtils.setField(alertService, "lokiApp", "test-api");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        RequestContext requestContext = new RequestContext("GET", "https://api.example.com/fail", "127.0.0.1", null, null, "trace-abc");
        ArgumentCaptor<DiscordMessage> captor = ArgumentCaptor.forClass(DiscordMessage.class);

        alertService.notify(500, new RuntimeException("fail"), requestContext);

        verify(discordAlertClient).send(captor.capture());
        String description = captor.getValue().embeds().getFirst().description();
        assertThat(description)
                .contains("traceId: trace-abc")
                .contains("http://grafana.example.com/explore?schemaVersion=1&panes=")
                .contains("trace-abc");
    }

    @Test
    void notifyOmitsLogLinkSectionWhenBaseUrlMissing() {
        RequestContext requestContext = new RequestContext("GET", "https://api.example.com/fail", "127.0.0.1", null, null, "trace-xyz");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        ArgumentCaptor<DiscordMessage> captor = ArgumentCaptor.forClass(DiscordMessage.class);

        alertService.notify(500, new RuntimeException("fail"), requestContext);

        verify(discordAlertClient).send(captor.capture());
        assertThat(captor.getValue().embeds().getFirst().description()).doesNotContain("### Grafana 로그");
    }
}
