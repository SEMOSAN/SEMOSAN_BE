package com.semosan.api.domain.mountain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.domain.mountain.dto.response.SlopeSegmentResponse;
import com.semosan.api.domain.mountain.enums.SlopeGrade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * courseSlopeSegments 캐시가 courseId 기준으로 실제 Spring 프록시를 타는지 확인한다. (#333)
 * 두 번째 호출에 courseId는 같지만 polyline/altitudes를 다르게 넣어도, 캐시가 걸려있으면
 * 재계산하지 않고 첫 결과를 그대로 반환해야 한다.
 */
@ExtendWith(SpringExtension.class)
class CourseSlopeSegmentCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("courseSlopeSegments");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CourseSlopeSegmentCalculator courseSlopeSegmentCalculator(ObjectMapper objectMapper) {
            return new CourseSlopeSegmentCalculator(objectMapper);
        }
    }

    private static final String FLAT_TWO_POINTS = """
            {"coordinates":[[127.0000,37.0000],[127.0010,37.0000]]}
            """;
    // 스무딩 윈도우(5)에 묻히지 않도록 충분히 긴 polyline으로 뚜렷한 오르막을 만든다.
    private static final String STEEP_POLYLINE = """
            {"coordinates":[
              [127.0000,37.0000],[127.0010,37.0000],[127.0020,37.0000],[127.0030,37.0000],
              [127.0040,37.0000],[127.0050,37.0000],[127.0060,37.0000],[127.0070,37.0000]
            ]}
            """;
    private static final String STEEP_ALTITUDES = "[0,0,0,0,1000,1000,1000,1000]";

    @Test
    void secondCallWithSameCourseIdReturnsCachedResultEvenWithDifferentInput() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            CourseSlopeSegmentCalculator calculator = ctx.getBean(CourseSlopeSegmentCalculator.class);

            List<SlopeSegmentResponse> first = calculator.calculate(1L, FLAT_TWO_POINTS, "[100,100]");
            assertThat(first).containsExactly(new SlopeSegmentResponse(0, 1, SlopeGrade.FLAT));

            // 같은 courseId, 완전히 다른 polyline/altitudes → 캐시 hit이면 재계산 없이 first가 그대로 나와야 함
            List<SlopeSegmentResponse> second = calculator.calculate(1L, STEEP_POLYLINE, STEEP_ALTITUDES);
            assertThat(second).isEqualTo(first);

            // 다른 courseId는 별도 캐시 엔트리 → 새로 계산됨 (오르막 구간이 포함돼야 함)
            List<SlopeSegmentResponse> differentCourse = calculator.calculate(2L, STEEP_POLYLINE, STEEP_ALTITUDES);
            assertThat(differentCourse).anyMatch(segment -> segment.grade() == SlopeGrade.STEEP_UP);
        }
    }
}
