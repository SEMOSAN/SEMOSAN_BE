package com.semosan.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Map<String, String> original = MDC.getCopyOfContextMap();
        long startTime = System.currentTimeMillis();
        try {
            MDC.put(TRACE_ID, resolveTraceId(request));
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsed);

            if (original != null) {
                MDC.setContextMap(original);
            } else {
                MDC.clear();
            }
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && requestId.matches(UUID_REGEX)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
