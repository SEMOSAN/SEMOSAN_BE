package com.semosan.api.common.alert;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public record RequestContext(
        String method,
        String url,
        String ip,
        String userId,
        String userAgent,
        String traceId
) {

    public static RequestContext from(HttpServletRequest request) {
        return new RequestContext(
                request.getMethod(),
                request.getRequestURL().toString(),
                clientIp(request),
                userId(request),
                request.getHeader("User-Agent"),
                // notify()가 @Async로 다른 스레드에서 실행되므로 MDC는 여기서 미리 꺼내둔다
                MDC.get("traceId")
        );
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String userId(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return null;
        }
        return request.getUserPrincipal().getName();
    }
}
