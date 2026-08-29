package com.semosan.api.domain.mountain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.config.RedisConfig;
import com.semosan.api.domain.mountain.dto.response.SlopeSegmentResponse;
import com.semosan.api.domain.mountain.enums.SlopeGrade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * courseSlopeSegments 캐시가 RedisConfig.cacheManager()(실제 RedisCacheManager)로도
 * SlopeSegmentResponse 직렬화/역직렬화까지 정상 동작하는지 확인한다. (#333)
 *
 * CourseSlopeSegmentCacheTest는 ConcurrentMapCacheManager로 프록시 동작만 빠르게 검증하고,
 * 이 테스트는 실제 Redis 라운드트립(직렬화 포함)을 검증한다. 로컬 Redis가 없으면 이 테스트만
 * 인프라 부재로 실패한다 — 코드 문제와 별개로 봐야 한다.
 */
@SpringBootTest(classes = CourseSlopeSegmentRedisCacheTest.TestConfig.class)
@ActiveProfiles("test")
class CourseSlopeSegmentRedisCacheTest {

    @Autowired
    private CourseSlopeSegmentCalculator calculator;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String STEEP_POLYLINE = """
            {"coordinates":[
              [127.0000,37.0000],[127.0010,37.0000],[127.0020,37.0000],[127.0030,37.0000],
              [127.0040,37.0000],[127.0050,37.0000],[127.0060,37.0000],[127.0070,37.0000]
            ]}
            """;
    private static final String STEEP_ALTITUDES = "[0,0,0,0,1000,1000,1000,1000]";

    @Test
    void resultSurvivesRealRedisRoundTrip() {
        long courseId = 999_001L;
        stringRedisTemplate.delete("courseSlopeSegments::" + courseId);

        List<SlopeSegmentResponse> first = calculator.calculate(courseId, STEEP_POLYLINE, STEEP_ALTITUDES);
        assertThat(first).anyMatch(segment -> segment.grade() == SlopeGrade.STEEP_UP);

        // 실제 Redis에 저장됐는지, 그리고 30분 TTL이 걸려있는지 직접 확인
        String redisKey = "courseSlopeSegments::" + courseId;
        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
        Long ttlSeconds = stringRedisTemplate.getExpire(redisKey);
        assertThat(ttlSeconds).isGreaterThan(0).isLessThanOrEqualTo(1800);

        // 다른 입력을 넣어도 캐시 hit이면 역직렬화된 first와 동일한 값이 나와야 함
        List<SlopeSegmentResponse> second = calculator.calculate(courseId, "{\"coordinates\":[[0,0],[0,0]]}", "[0,0]");
        assertThat(second).isEqualTo(first);

        stringRedisTemplate.delete(redisKey);
    }

    @EnableAutoConfiguration
    @Import({RedisConfig.class, CourseSlopeSegmentCalculator.class})
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
