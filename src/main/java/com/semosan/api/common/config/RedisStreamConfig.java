package com.semosan.api.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final StringRedisTemplate redisTemplate;
    private final TrackingProperties trackingProperties;

    @PostConstruct
    public void createConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(
                    trackingProperties.getStreamKey(),
                    ReadOffset.from("0"),
                    trackingProperties.getConsumerGroup()
            );
        } catch (RedisSystemException e) {
            // consumer group already exists
        }
    }

    @Scheduled(fixedDelay = 60_000L)
    public void trimStream() {
        Long trimmed = redisTemplate.opsForStream()
                .trim(trackingProperties.getStreamKey(), trackingProperties.getStreamMaxLen(), true);
        if (trimmed != null && trimmed > 0) {
            log.info("[STREAM] 트리밍 완료 | {}건 제거 | maxLen={}", trimmed, trackingProperties.getStreamMaxLen());
        }
    }
}
