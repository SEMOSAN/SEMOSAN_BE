package com.semosan.api.common.alert;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public record RequestContext(
        String method,
        String url,
        String ip,
        String userId,
        String userAgent
) {

    public static RequestContext from(HttpServletRequest request) {
        return new RequestContext(
                request.getMethod(),
                request.getRequestURL().toString(),
                clientIp(request),
                userId(request),
                request.getHeader("User-Agent")
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
