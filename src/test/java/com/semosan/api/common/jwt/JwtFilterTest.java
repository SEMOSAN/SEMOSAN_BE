package com.semosan.api.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilterSetsAuthenticationForValidUserToken() throws Exception {
        Claims claims = mock(Claims.class);
        User user = User.createTestUser("user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(jwtService.validateAccessTokenAndGetClaims("access")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access")).thenReturn(false);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(claims.get("tokenType", String.class)).thenReturn(null);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("/api/mountains", "Bearer access");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void doFilterSetsAdminAuthorityForAdminToken() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.validateAccessTokenAndGetClaims("admin-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("admin-token")).thenReturn(false);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(9L);
        when(claims.get("tokenType", String.class)).thenReturn("ADMIN");
        MockHttpServletRequest request = request("/api/admin/mountains", "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        verify(userRepository, never()).findByIdAndDeletedFalse(9L);
    }

    @Test
    void doFilterPassesThroughWhenTokenIsMissing() throws Exception {
        MockHttpServletRequest request = request("/api/mountains", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).validateAccessTokenAndGetClaims(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilterSkipsExcludedPath() throws Exception {
        MockHttpServletRequest request = request("/api/auth/test/login", "Bearer ignored");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        verify(jwtService, never()).validateAccessTokenAndGetClaims(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilterWritesErrorResponseWhenTokenIsBlacklisted() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.validateAccessTokenAndGetClaims("access")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access")).thenReturn(true);
        MockHttpServletRequest request = request("/api/mountains", "Bearer access");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(ErrorStatus.JWT_BLACKLISTED.getHttpStatus().value());
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains(ErrorStatus.JWT_BLACKLISTED.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterWritesErrorResponseWhenUserIsSuspended() throws Exception {
        Claims claims = mock(Claims.class);
        User user = User.createTestUser("user", DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", 1L);
        user.suspend(java.time.LocalDateTime.now().plusDays(1));
        when(jwtService.validateAccessTokenAndGetClaims("access")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access")).thenReturn(false);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(claims.get("tokenType", String.class)).thenReturn(null);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = request("/api/mountains", "Bearer access");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(ErrorStatus.USER_SUSPENDED.getHttpStatus().value());
        assertThat(response.getContentAsString()).contains(ErrorStatus.USER_SUSPENDED.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterWritesErrorResponseWhenJwtServiceThrows() throws Exception {
        when(jwtService.validateAccessTokenAndGetClaims("bad"))
                .thenThrow(new GeneralException(ErrorStatus.JWT_MALFORMED));
        MockHttpServletRequest request = request("/api/mountains", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(ErrorStatus.JWT_MALFORMED.getHttpStatus().value());
        assertThat(response.getContentAsString()).contains(ErrorStatus.JWT_MALFORMED.getCode());
    }

    @Test
    void doFilterWritesErrorResponseWhenUserIsWithdrawnOrNotFound() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.validateAccessTokenAndGetClaims("access")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access")).thenReturn(false);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(claims.get("tokenType", String.class)).thenReturn(null);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        MockHttpServletRequest request = request("/api/mountains", "Bearer access");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(ErrorStatus.JWT_USER_WITHDRAWN.getHttpStatus().value());
        assertThat(response.getContentAsString()).contains(ErrorStatus.JWT_USER_WITHDRAWN.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private JwtFilter filter() {
        return new JwtFilter(jwtService, objectMapper, userRepository);
    }

    private MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
