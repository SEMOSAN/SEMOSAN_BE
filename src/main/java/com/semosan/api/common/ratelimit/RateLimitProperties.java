package com.semosan.api.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 레이트리밋 정책 설정. application.yaml 의 {@code rate-limit} 블록에서 바인딩된다.
 *
 * @param enabled 레이트리밋 전체 on/off
 * @param global  전역 기본 정책 (/api/** 대상)
 * @param auth    인증 엔드포인트(로그인/OAuth/토큰재발급) 강화 정책
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Rule global,
        Rule auth
) {

    /**
     * @param limit         윈도우당 허용 요청 수
     * @param windowSeconds 윈도우 길이(초)
     */
    public record Rule(int limit, long windowSeconds) {
    }
}
