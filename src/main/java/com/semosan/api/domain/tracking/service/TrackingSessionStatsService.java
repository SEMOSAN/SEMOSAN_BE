package com.semosan.api.domain.tracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 트래킹 진행 중 실시간 통계를 Redis Hash 에 누적 갱신한다.
 *  - 키: tracking:session:{sessionId}:stats
 *  - 필드: last_lat, last_lng, last_altitude, last_recorded_at,
 *          distance_total(m), ascent_total(m), descent_total(m), point_count
 *  - TTL: 24h (세션 자동 만료와 정렬)
 *  - 용도: #46 라이브 액티비티 푸시, 고도 임계 감지, 종료 시 빠른 통계 조회
 *
 * TODO: 다중 인스턴스/동시성을 고려해야 한다면 Lua 스크립트로 atomic read-modify-write 로 교체.
 */
@Service
@RequiredArgsConstructor
public class TrackingSessionStatsService {

    private static final String KEY_PREFIX = "tracking:session:";
    private static final String STATS_SUFFIX = ":stats";
    private static final Duration TTL = Duration.ofHours(24);

    // 지구 반지름 (m)
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private static final String F_LAST_LAT = "last_lat";
    private static final String F_LAST_LNG = "last_lng";
    private static final String F_LAST_ALTITUDE = "last_altitude";
    private static final String F_LAST_RECORDED_AT = "last_recorded_at";
    private static final String F_DISTANCE_TOTAL = "distance_total";
    private static final String F_ASCENT_TOTAL = "ascent_total";
    private static final String F_DESCENT_TOTAL = "descent_total";
    private static final String F_MAX_ALTITUDE = "max_altitude";
    private static final String F_POINT_COUNT = "point_count";

    private final StringRedisTemplate redisTemplate;

    /** 점을 누적 갱신하고, 갱신 후 총 거리(m)를 반환한다 — 호출자에서 마일스톤 트리거에 활용. */
    public double recordPoint(Long sessionId, double lat, double lng, Double altitude, LocalDateTime recordedAt) {
        String key = statsKey(sessionId);
        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        Map<String, String> prev = hash.entries(key);

        double distanceTotal = parseDouble(prev.get(F_DISTANCE_TOTAL));
        double ascentTotal = parseDouble(prev.get(F_ASCENT_TOTAL));
        double descentTotal = parseDouble(prev.get(F_DESCENT_TOTAL));
        Double maxAltitude = parseNullableDouble(prev.get(F_MAX_ALTITUDE));
        long pointCount = parseLong(prev.get(F_POINT_COUNT));

        Double prevLat = parseNullableDouble(prev.get(F_LAST_LAT));
        Double prevLng = parseNullableDouble(prev.get(F_LAST_LNG));
        if (prevLat != null && prevLng != null) {
            distanceTotal += haversineMeters(prevLat, prevLng, lat, lng);
        }
        Double prevAltitude = parseNullableDouble(prev.get(F_LAST_ALTITUDE));
        if (altitude != null && prevAltitude != null) {
            double delta = altitude - prevAltitude;
            if (delta > 0) {
                ascentTotal += delta;
            } else if (delta < 0) {
                descentTotal += -delta;
            }
        }
        if (altitude != null && (maxAltitude == null || altitude > maxAltitude)) {
            maxAltitude = altitude;
        }
        pointCount += 1;

        Map<String, String> next = new HashMap<>();
        next.put(F_LAST_LAT, String.valueOf(lat));
        next.put(F_LAST_LNG, String.valueOf(lng));
        if (altitude != null) {
            next.put(F_LAST_ALTITUDE, String.valueOf(altitude));
        }
        next.put(F_LAST_RECORDED_AT, recordedAt.toString());
        next.put(F_DISTANCE_TOTAL, String.valueOf(distanceTotal));
        next.put(F_ASCENT_TOTAL, String.valueOf(ascentTotal));
        next.put(F_DESCENT_TOTAL, String.valueOf(descentTotal));
        if (maxAltitude != null) {
            next.put(F_MAX_ALTITUDE, String.valueOf(maxAltitude));
        }
        next.put(F_POINT_COUNT, String.valueOf(pointCount));

        hash.putAll(key, next);
        redisTemplate.expire(key, TTL);
        return distanceTotal;
    }

    public static String statsKey(Long sessionId) {
        return KEY_PREFIX + sessionId + STATS_SUFFIX;
    }

    /**
     * 세션 종료 시 통계 스냅샷 조회 — HikingRecord 생성에 사용된다.
     * 점이 한 번도 들어오지 않았으면 모든 필드 0/null.
     */
    public Stats getStats(Long sessionId) {
        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        Map<String, String> entries = hash.entries(statsKey(sessionId));
        return new Stats(
                parseDouble(entries.get(F_DISTANCE_TOTAL)),
                parseDouble(entries.get(F_ASCENT_TOTAL)),
                parseDouble(entries.get(F_DESCENT_TOTAL)),
                parseNullableDouble(entries.get(F_MAX_ALTITUDE)),
                parseLong(entries.get(F_POINT_COUNT))
        );
    }

    public record Stats(
            double distanceMeters,
            double ascentMeters,
            double descentMeters,
            Double maxAltitudeMeters,
            long pointCount
    ) {
    }

    /** Haversine 거리(m). */
    static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static double parseDouble(String value) {
        return (value == null || value.isEmpty()) ? 0.0 : Double.parseDouble(value);
    }

    private static Double parseNullableDouble(String value) {
        return (value == null || value.isEmpty()) ? null : Double.parseDouble(value);
    }

    private static long parseLong(String value) {
        return (value == null || value.isEmpty()) ? 0L : Long.parseLong(value);
    }
}
