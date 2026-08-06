package com.semosan.api.common.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    @Test
    void resolveTokenReturnsBearerToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        String token = JwtUtil.resolveToken(request);

        assertThat(token).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveTokenReturnsNullWhenHeaderIsMissingOrNotBearer() {
        HttpServletRequest missing = mock(HttpServletRequest.class);
        HttpServletRequest basic = mock(HttpServletRequest.class);
        when(missing.getHeader("Authorization")).thenReturn(null);
        when(basic.getHeader("Authorization")).thenReturn("Basic token");

        assertThat(JwtUtil.resolveToken(missing)).isNull();
        assertThat(JwtUtil.resolveToken(basic)).isNull();
    }
}
