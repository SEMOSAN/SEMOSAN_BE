package com.semosan.api.common.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterUsesRequestIdHeaderAndRestoresOriginalMdc() throws Exception {
        MDC.put("existing", "value");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mountains");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", "123e4567-e89b-12d3-a456-426614174000");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("traceId")).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
            assertThat(MDC.get("method")).isEqualTo("GET");
            assertThat(MDC.get("uri")).isEqualTo("/mountains");
        });

        assertThat(MDC.getCopyOfContextMap()).containsEntry("existing", "value");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void doFilterGeneratesTraceIdWhenHeaderIsInvalidAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", "not-uuid");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("traceId")).isNotBlank();
            assertThat(MDC.get("traceId")).isNotEqualTo("not-uuid");
        });

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void doFilterGeneratesTraceIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("traceId")).isNotBlank();
            assertThat(MDC.get("method")).isEqualTo("GET");
            assertThat(MDC.get("uri")).isEqualTo("/health");
        });

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void doFilterRestoresMdcEvenWhenChainThrows() {
        MDC.put("existing", "value");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/fail");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new IllegalStateException("boom");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.getCopyOfContextMap()).containsEntry("existing", "value");
    }
}
