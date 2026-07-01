package com.semosan.api.domain.tracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 트래킹 진행 중 실시간 통계를 Redis Hash 에 누적 갱신한다.
 *  - 키: tracking:session:{sessionId}:stats
 *  - 필드: last_lat, last_lng, last_altitude, last_recorded_at,
 *          distance_total(m), ascent_total(m), descent_total(m), point_count
 *  - TTL: 24h (세션 자동 만료와 정렬)
 *  - 용도: #46 라이브 액티비티 푸시, 고도 임계 감지, 종료 시 빠른 통계 조회
 *
 * Lua 스크립트로 atomic read-modify-write 보장 — 동시성 안전.
 */
@Service
@RequiredArgsConstructor
public class TrackingSessionStatsService {

    private static final String KEY_PREFIX = "tracking:session:";
    private static final String STATS_SUFFIX = ":stats";
    private static final Duration TTL = Duration.ofHours(24);

    private static final String F_DISTANCE_TOTAL = "distance_total";
    private static final String F_ASCENT_TOTAL = "ascent_total";
    private static final String F_DESCENT_TOTAL = "descent_total";
    private static final String F_MAX_ALTITUDE = "max_altitude";
    private static final String F_POINT_COUNT = "point_count";

    private static final DefaultRedisScript<String> RECORD_POINT_SCRIPT;

    static {
        RECORD_POINT_SCRIPT = new DefaultRedisScript<>();
        RECORD_POINT_SCRIPT.setLocation(new ClassPathResource("redis/tracking-stats-update.lua"));
        RECORD_POINT_SCRIPT.setResultType(String.class);
    }

    private final StringRedisTemplate redisTemplate;

    /** 점을 누적 갱신하고, 갱신 후 총 거리(m)를 반환한다 — Lua 스크립트로 atomic 처리. */
    public double recordPoint(Long sessionId, double lat, double lng, Double altitude, LocalDateTime recordedAt) {
        String key = statsKey(sessionId);
        String result = redisTemplate.execute(
                RECORD_POINT_SCRIPT,
                List.of(key),
                String.valueOf(lat),
                String.valueOf(lng),
                altitude != null ? String.valueOf(altitude) : "",
                recordedAt.toString(),
                String.valueOf(TTL.toSeconds())
        );
        return result != null ? Double.parseDouble(result) : 0.0;
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

    public record LastPosition(Double lat, Double lng, Double altitude) {
        public boolean isEmpty() {
            return lat == null || lng == null;
        }
    }

    public LastPosition getLastPosition(Long sessionId) {
        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        String key = statsKey(sessionId);
        List<String> values = hash.multiGet(key, List.of("last_lat", "last_lng", "last_altitude"));
        if (values == null || values.size() < 3) {
            return new LastPosition(null, null, null);
        }
        return new LastPosition(
                parseNullableDouble(values.get(0)),
                parseNullableDouble(values.get(1)),
                parseNullableDouble(values.get(2))
        );
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
