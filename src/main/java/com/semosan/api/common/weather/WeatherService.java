package com.semosan.api.common.weather;

import com.semosan.api.common.util.GridConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WeatherService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    private final WebClient webClient;
    private final String apiKey;

    public WeatherService(
            @Qualifier("weatherApiWebClient") WebClient webClient,
            @Value("${weather.api-key}") String apiKey
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    public Optional<Double> getTemperature(double latitude, double longitude) {
        try {
            GridConverter.Grid grid = GridConverter.toGrid(latitude, longitude);
            LocalDateTime now = adjustBaseTime(LocalDateTime.now());

            String baseDate = now.format(DATE_FMT);
            String baseTime = now.format(TIME_FMT);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getUltraSrtNcst")
                            .queryParam("serviceKey", apiKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 10)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", grid.nx())
                            .queryParam("ny", grid.ny())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(3));

            return extractTemperature(response);
        } catch (Exception e) {
            log.warn("기상청 API 호출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Double> extractTemperature(Map<String, Object> response) {
        if (response == null) return Optional.empty();

        Map<String, Object> root = (Map<String, Object>) response.get("response");
        if (root == null) return Optional.empty();

        Map<String, Object> body = (Map<String, Object>) root.get("body");
        if (body == null) return Optional.empty();

        Map<String, Object> items = (Map<String, Object>) body.get("items");
        if (items == null) return Optional.empty();

        List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("item");
        if (itemList == null) return Optional.empty();

        return itemList.stream()
                .filter(item -> "T1H".equals(item.get("category")))
                .findFirst()
                .map(item -> Double.parseDouble(String.valueOf(item.get("obsrValue"))));
    }

    private LocalDateTime adjustBaseTime(LocalDateTime now) {
        if (now.getMinute() < 40) {
            now = now.minusHours(1);
        }
        return now.withMinute(0).withSecond(0).withNano(0);
    }
}
