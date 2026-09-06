package com.semosan.api.common.alert;

import com.semosan.api.common.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

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
                ClientIpResolver.resolve(request),
                userId(request),
                request.getHeader("User-Agent"),
                // notify()가 @Async로 다른 스레드에서 실행되므로 MDC는 여기서 미리 꺼내둔다
                MDC.get("traceId")
        );
    }

    private static String userId(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return null;
        }
        return request.getUserPrincipal().getName();
    }
}
