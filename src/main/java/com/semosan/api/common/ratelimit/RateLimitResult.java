package com.semosan.api.common.ratelimit;

/**
 * 레이트리밋 판정 결과.
 *
 * @param allowed           허용 여부 (true = 통과, false = 차단)
 * @param retryAfterSeconds 현재 윈도우가 끝날 때까지 남은 초 (Retry-After 헤더용)
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
}
