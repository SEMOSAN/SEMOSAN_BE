package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import com.semosan.api.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginProcessorTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private OAuthLoginProcessor processor;

    @Test
    void loginFindsOrRegistersUserAndIssuesTokens() {
        OAuthUserProfile profile = new OAuthUserProfile("oauth-id", "user@example.com", "사용자");
        User user = mock(User.class);
        when(userService.findOrRegisterOAuthUser(profile, OAuthProvider.KAKAO, DeviceType.IOS)).thenReturn(user);
        when(jwtService.issueTokens(user)).thenReturn(new TokenIssuance("access", "refresh"));
        when(user.getId()).thenReturn(1L);
        when(user.getOnboardingStatus()).thenReturn(OnboardingStatus.COMPLETE);

        OAuthLoginResponse response = processor.login(profile, OAuthProvider.KAKAO, DeviceType.IOS);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.onboardingCompleted()).isTrue();
        verify(userService).findOrRegisterOAuthUser(profile, OAuthProvider.KAKAO, DeviceType.IOS);
        verify(jwtService).issueTokens(user);
    }
}
