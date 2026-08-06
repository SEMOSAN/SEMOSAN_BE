package com.semosan.api.common.weather;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherServiceTest {

    private final WeatherService weatherService = new WeatherService(WebClient.builder().build(), "api-key");

    @Test
    void getTemperatureReturnsTemperatureFromWeatherApiResponse() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("""
                                {
                                  "response": {
                                    "body": {
                                      "items": {
                                        "item": [
                                          {"category": "T1H", "obsrValue": "21.5"}
                                        ]
                                      }
                                    }
                                  }
                                }
                                """)
                        .build()))
                .build();
        WeatherService service = new WeatherService(webClient, "api-key");

        Optional<Double> temperature = service.getTemperature(37.5665, 126.9780);

        assertThat(temperature).contains(21.5);
    }

    @Test
    void getTemperatureReturnsEmptyWhenWeatherApiFails() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("network")))
                .build();
        WeatherService service = new WeatherService(webClient, "api-key");

        Optional<Double> temperature = service.getTemperature(37.5665, 126.9780);

        assertThat(temperature).isEmpty();
    }

    @Test
    void extractTemperatureReturnsT1hObservation() {
        Map<String, Object> response = Map.of(
                "response", Map.of(
                        "body", Map.of(
                                "items", Map.of(
                                        "item", List.of(
                                                Map.of("category", "RN1", "obsrValue", "0"),
                                                Map.of("category", "T1H", "obsrValue", "23.4")
                                        )
                                )
                        )
                )
        );

        Optional<Double> temperature = extractTemperature(response);

        assertThat(temperature).contains(23.4);
    }

    @Test
    void extractTemperatureReturnsEmptyWhenResponseShapeIsMissing() {
        assertThat(extractTemperature(null)).isEmpty();
        assertThat(extractTemperature(Map.of())).isEmpty();
        assertThat(extractTemperature(Map.of("response", Map.of()))).isEmpty();
        assertThat(extractTemperature(Map.of("response", Map.of("body", Map.of())))).isEmpty();
        assertThat(extractTemperature(Map.of("response", Map.of("body", Map.of("items", Map.of()))))).isEmpty();
    }

    @Test
    void adjustBaseTimeUsesPreviousHourBeforeMinuteForty() {
        LocalDateTime adjusted = adjustBaseTime(LocalDateTime.of(2026, 8, 6, 13, 39, 59));

        assertThat(adjusted).isEqualTo(LocalDateTime.of(2026, 8, 6, 12, 0));
    }

    @Test
    void adjustBaseTimeUsesCurrentHourAtMinuteFortyOrLater() {
        LocalDateTime adjusted = adjustBaseTime(LocalDateTime.of(2026, 8, 6, 13, 40, 1));

        assertThat(adjusted).isEqualTo(LocalDateTime.of(2026, 8, 6, 13, 0));
    }

    @SuppressWarnings("unchecked")
    private Optional<Double> extractTemperature(Map<String, Object> response) {
        return (Optional<Double>) ReflectionTestUtils.invokeMethod(weatherService, "extractTemperature", response);
    }

    private LocalDateTime adjustBaseTime(LocalDateTime now) {
        return ReflectionTestUtils.invokeMethod(weatherService, "adjustBaseTime", now);
    }
}
