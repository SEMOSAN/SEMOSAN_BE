package com.semosan.api.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4는 기본 JSON 라이브러리로 Jackson 3(JsonMapper)을 자동 구성하며,
 * Jackson 2 {@link ObjectMapper} 빈은 더 이상 자동 등록되지 않는다.
 * jjwt-jackson 등 일부 서드파티 라이브러리가 여전히 Jackson 2 기반이라
 * 애플리케이션 코드에서 직접 주입받는 ObjectMapper는 이 빈으로 명시 구성한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
