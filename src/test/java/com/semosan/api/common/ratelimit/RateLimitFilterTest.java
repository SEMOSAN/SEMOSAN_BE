package com.semosan.api.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.status.ErrorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RedisRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passesThroughWhenWithinLimit() throws Exception {
        when(rateLimiter.tryConsume(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(true, 60));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request("/api/mountains"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void returnsTooManyRequestsWhenLimitExceeded() throws Exception {
        when(rateLimiter.tryConsume(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(false, 30));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request("/api/mountains"), response, chain);

        assertThat(response.getStatus()).isEqualTo(ErrorStatus.TOO_MANY_REQUESTS.getHttpStatus().value());
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains(ErrorStatus.TOO_MANY_REQUESTS.getCode());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void appliesAuthRuleToAuthEndpoints() throws Exception {
        when(rateLimiter.tryConsume(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(true, 60));

        filter().doFilter(request("/api/auth/token/reissue"), new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimiter).tryConsume(eq("auth"), anyString(), eq(30), eq(60L));
    }

    @Test
    void appliesGlobalRuleToNormalEndpoints() throws Exception {
        when(rateLimiter.tryConsume(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(true, 60));

        filter().doFilter(request("/api/mountains"), new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimiter).tryConsume(eq("global"), anyString(), eq(300), eq(60L));
    }

    @Test
    void usesFirstIpFromForwardedForHeader() throws Exception {
        when(rateLimiter.tryConsume(anyString(), anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(true, 60));
        MockHttpServletRequest request = request("/api/mountains");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimiter).tryConsume(anyString(), eq("203.0.113.9"), anyInt(), anyLong());
    }

    @Test
    void skipsExcludedPath() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request("/swagger-ui.html"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(rateLimiter, never()).tryConsume(anyString(), anyString(), anyInt(), anyLong());
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        RateLimitFilter disabledFilter =
                new RateLimitFilter(properties(false), rateLimiter, objectMapper);

        disabledFilter.doFilter(request("/api/mountains"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(rateLimiter, never()).tryConsume(anyString(), anyString(), anyInt(), anyLong());
    }

    private RateLimitFilter filter() {
        return new RateLimitFilter(properties(true), rateLimiter, objectMapper);
    }

    private static RateLimitProperties properties(boolean enabled) {
        return new RateLimitProperties(
                enabled,
                new RateLimitProperties.Rule(300, 60),
                new RateLimitProperties.Rule(30, 60));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }
}
