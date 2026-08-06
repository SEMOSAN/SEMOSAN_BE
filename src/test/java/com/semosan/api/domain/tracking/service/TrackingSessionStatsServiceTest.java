package com.semosan.api.domain.tracking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingSessionStatsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    void recordPointReturnsDistanceFromLuaScript() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("tracking:session:1:stats")),
                eq("37.5"),
                eq("127.0"),
                eq("100.0"),
                eq("2026-08-06T10:00"),
                eq("86400")
        )).thenReturn("123.45");

        double result = new TrackingSessionStatsService(redisTemplate)
                .recordPoint(1L, 37.5, 127.0, 100.0, LocalDateTime.of(2026, 8, 6, 10, 0));

        assertThat(result).isEqualTo(123.45);
    }

    @Test
    void recordPointReturnsZeroWhenLuaScriptReturnsNullAndAltitudeIsNull() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("tracking:session:1:stats")),
                eq("37.5"),
                eq("127.0"),
                eq(""),
                eq("2026-08-06T10:00"),
                eq("86400")
        )).thenReturn(null);

        double result = new TrackingSessionStatsService(redisTemplate)
                .recordPoint(1L, 37.5, 127.0, null, LocalDateTime.of(2026, 8, 6, 10, 0));

        assertThat(result).isZero();
    }

    @Test
    void getStatsParsesHashEntries() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("tracking:session:1:stats")).thenReturn(Map.of(
                "distance_total", "1200.5",
                "ascent_total", "100.0",
                "descent_total", "80.0",
                "max_altitude", "650.0",
                "point_count", "5"
        ));

        TrackingSessionStatsService.Stats result = new TrackingSessionStatsService(redisTemplate).getStats(1L);

        assertThat(result.distanceMeters()).isEqualTo(1200.5);
        assertThat(result.ascentMeters()).isEqualTo(100.0);
        assertThat(result.descentMeters()).isEqualTo(80.0);
        assertThat(result.maxAltitudeMeters()).isEqualTo(650.0);
        assertThat(result.pointCount()).isEqualTo(5L);
    }

    @Test
    void getStatsUsesZeroDefaultsForMissingOrEmptyEntries() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("tracking:session:1:stats")).thenReturn(Map.of(
                "distance_total", "",
                "ascent_total", "",
                "descent_total", "",
                "max_altitude", "",
                "point_count", ""
        ));

        TrackingSessionStatsService.Stats result = new TrackingSessionStatsService(redisTemplate).getStats(1L);

        assertThat(result.distanceMeters()).isZero();
        assertThat(result.ascentMeters()).isZero();
        assertThat(result.descentMeters()).isZero();
        assertThat(result.maxAltitudeMeters()).isNull();
        assertThat(result.pointCount()).isZero();
    }

    @Test
    void getLastPositionReturnsEmptyWhenRedisResultIsNull() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("tracking:session:1:stats", List.of("last_lat", "last_lng", "last_altitude")))
                .thenReturn(null);

        TrackingSessionStatsService.LastPosition result =
                new TrackingSessionStatsService(redisTemplate).getLastPosition(1L);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.altitude()).isNull();
    }

    @Test
    void getLastPositionReturnsEmptyWhenRedisResultIsIncomplete() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("tracking:session:1:stats", List.of("last_lat", "last_lng", "last_altitude")))
                .thenReturn(List.of("37.5", "127.0"));

        TrackingSessionStatsService.LastPosition result =
                new TrackingSessionStatsService(redisTemplate).getLastPosition(1L);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void getLastPositionParsesValuesAndEmptyAltitudeAsNull() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("tracking:session:1:stats", List.of("last_lat", "last_lng", "last_altitude")))
                .thenReturn(List.of("37.5", "127.0", ""));

        TrackingSessionStatsService.LastPosition result =
                new TrackingSessionStatsService(redisTemplate).getLastPosition(1L);

        assertThat(result.lat()).isEqualTo(37.5);
        assertThat(result.lng()).isEqualTo(127.0);
        assertThat(result.altitude()).isNull();
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    void statsKeyReturnsRedisStatsKey() {
        assertThat(TrackingSessionStatsService.statsKey(10L)).isEqualTo("tracking:session:10:stats");
    }
}
