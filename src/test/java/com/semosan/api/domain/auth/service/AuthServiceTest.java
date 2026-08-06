package com.semosan.api.domain.auth.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.auth.dto.response.ReissueResponse;
import com.semosan.api.domain.auth.event.UserWithdrawCleanupRequestedEvent;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.service.UserReader;
import com.semosan.api.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserReader userReader;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, userReader, jwtService, eventPublisher);
        ReflectionTestUtils.setField(authService, "testSecretKey", "secret");
    }

    @Test
    void loginIssuesTokensWhenSecretMatches() {
        User user = user(1L, OnboardingStatus.COMPLETE);
        LoginRequest request = new LoginRequest("test-user", DeviceType.IOS, "secret");
        when(userService.findOrCreateTestUser("test-user", DeviceType.IOS)).thenReturn(user);
        when(jwtService.issueTokens(user)).thenReturn(new TokenIssuance("access", "refresh"));

        LoginResponse response = authService.login(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.onboardingCompleted()).isTrue();
    }

    @Test
    void loginThrowsWhenSecretDoesNotMatch() {
        LoginRequest request = new LoginRequest("test-user", DeviceType.IOS, "wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FORBIDDEN);
        verify(userService, never()).findOrCreateTestUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void reissueValidatesRefreshTokenAndIssuesNewTokens() {
        Claims claims = mock(Claims.class);
        User user = user(2L, OnboardingStatus.INCOMPLETE);
        when(jwtService.validateRefreshTokenSignature("refresh")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("2");
        when(userReader.findActiveUserById(2L)).thenReturn(user);
        when(jwtService.issueTokens(user)).thenReturn(new TokenIssuance("new-access", "new-refresh"));

        ReissueResponse response = authService.reissue("refresh");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(jwtService).validateRefreshToken("refresh", 2L);
    }

    @Test
    void logoutBlacklistsAccessTokenAndDeletesRefreshToken() {
        authService.logout(1L, "access");

        verify(jwtService).blacklistAccessToken("access");
        verify(jwtService).deleteRefreshToken(1L);
    }

    @Test
    void withdrawWithdrawsUserAndPublishesCleanupEvent() {
        User user = user(1L, OnboardingStatus.COMPLETE);
        when(userReader.findActiveUserById(1L)).thenReturn(user);
        ArgumentCaptor<UserWithdrawCleanupRequestedEvent> captor =
                ArgumentCaptor.forClass(UserWithdrawCleanupRequestedEvent.class);

        authService.withdraw(1L, "access");

        verify(userService).withdrawUser(user);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().accessToken()).isEqualTo("access");
    }

    private User user(Long id, OnboardingStatus onboardingStatus) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "onboardingStatus", onboardingStatus);
        return user;
    }
}
