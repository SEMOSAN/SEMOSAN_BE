package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.common.config.TrackingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingStreamTrimScheduler {

    private final StringRedisTemplate redisTemplate;
    private final TrackingProperties trackingProperties;

    @Scheduled(fixedDelay = 60_000L)
    public void trimStream() {
        try {
            Long trimmed = redisTemplate.opsForStream()
                    .trim(trackingProperties.getStreamKey(), trackingProperties.getStreamMaxLen(), true);
            if (trimmed != null && trimmed > 0) {
                log.info("[STREAM] 트리밍 완료 | {}건 제거 | maxLen={}", trimmed, trackingProperties.getStreamMaxLen());
            }
        } catch (Exception e) {
            log.warn("[STREAM] 트리밍 실패", e);
        }
    }
}
