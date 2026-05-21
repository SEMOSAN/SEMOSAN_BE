package com.semosan.api.domain.tracking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 트래킹 세션의 마지막 GPS 활동 시각을 Redis 에 기록.
 *  - 키: tracking:session:{sessionId}:lastActive
 *  - 값: ISO LocalDateTime
 *  - TTL: 25h (24h 만료 정책 + 1h 안전 마진)
 *  - 용도: 24h 자동 만료 스케줄러의 stale 판정 — GPS 수집이 Redis Stream → DB(tracking_points)
 *    경로라 tracking_sessions.updated_at 이 갱신되지 않으므로 DB updatedAt 대신 이 값을 사용한다.
 */
@Service
@RequiredArgsConstructor
public class TrackingSessionActivityService {

    private static final String KEY_PREFIX = "tracking:session:";
    private static final String ACTIVITY_SUFFIX = ":lastActive";
    private static final Duration TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;

    /** GPS 점 수신 시 호출 — 현재 시각으로 마킹하고 TTL 갱신. */
    public void markActive(Long sessionId) {
        redisTemplate.opsForValue().set(activityKey(sessionId), LocalDateTime.now().toString(), TTL);
    }

    /** 마지막 활동 시각 조회. 키 없거나 파싱 실패 시 empty. */
    public Optional<LocalDateTime> getLastActive(Long sessionId) {
        String value = redisTemplate.opsForValue().get(activityKey(sessionId));
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String activityKey(Long sessionId) {
        return KEY_PREFIX + sessionId + ACTIVITY_SUFFIX;
    }
}
