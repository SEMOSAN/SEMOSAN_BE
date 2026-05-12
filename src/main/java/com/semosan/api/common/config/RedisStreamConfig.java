package com.semosan.api.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

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
}
