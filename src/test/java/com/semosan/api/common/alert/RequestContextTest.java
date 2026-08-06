package com.semosan.api.common.alert;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestContextTest {

    @Test
    void fromUsesFirstForwardedForIpAndPrincipalName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = () -> "1";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://api.example.com/mountains"));
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getUserPrincipal()).thenReturn(principal);

        RequestContext context = RequestContext.from(request);

        assertThat(context.method()).isEqualTo("GET");
        assertThat(context.url()).isEqualTo("https://api.example.com/mountains");
        assertThat(context.ip()).isEqualTo("203.0.113.1");
        assertThat(context.userId()).isEqualTo("1");
        assertThat(context.userAgent()).isEqualTo("JUnit");
    }

    @Test
    void fromFallsBackToRemoteAddrWhenForwardedForIsBlank() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://api.example.com/login"));
        when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        RequestContext context = RequestContext.from(request);

        assertThat(context.ip()).isEqualTo("127.0.0.1");
        assertThat(context.userId()).isNull();
    }
}
