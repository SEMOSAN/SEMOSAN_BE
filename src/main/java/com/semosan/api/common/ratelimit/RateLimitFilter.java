package com.semosan.api.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.config.SecurityConfig;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.ErrorStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    // 레이트리밋 제외: 스웨거, WebSocket 핸드셰이크(STOMP CONNECT 에서 별도 인증/제한 대상)
    private static final List<PathPatternRequestMatcher> EXCLUDED_PATHS =
            Stream.of(
                            SecurityConfig.SWAGGER_URIS,
                            SecurityConfig.WEBSOCKET_URIS
                    )
                    .flatMap(Arrays::stream)
                    .map(PathPatternRequestMatcher.withDefaults()::matcher)
                    .toList();

    // 인증 관련 엔드포인트: brute force 방어를 위해 강화된 한도 적용
    private static final List<PathPatternRequestMatcher> AUTH_PATHS =
            Stream.of(
                            SecurityConfig.AUTH_URIS,
                            SecurityConfig.OAUTH_URIS,
                            SecurityConfig.ADMIN_PUBLIC_URIS
                    )
                    .flatMap(Arrays::stream)
                    .map(PathPatternRequestMatcher.withDefaults()::matcher)
                    .toList();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        return EXCLUDED_PATHS.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean isAuth = AUTH_PATHS.stream().anyMatch(matcher -> matcher.matches(request));
        RateLimitProperties.Rule rule = isAuth ? properties.auth() : properties.global();
        String scope = isAuth ? "auth" : "global";
        String clientIp = clientIp(request);

        RateLimitResult result = rateLimiter.tryConsume(
                scope, clientIp, rule.limit(), rule.windowSeconds());

        if (!result.allowed()) {
            log.warn("[*] RateLimit 초과 차단 (scope={}, ip={}, uri={})",
                    scope, clientIp, request.getRequestURI());
            sendTooManyRequests(response, result.retryAfterSeconds());
            return;
        }
        filterChain.doFilter(request, response);
    }

    // RequestContext.clientIp 와 동일한 X-Forwarded-For 우선 규칙
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ErrorStatus status = ErrorStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ApiResponse<Void> body = new ApiResponse<>(
                false,
                status.getCode(),
                status.getMessage(),
                null
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
