package com.semosan.api.domain.oauth.service;

import com.semosan.api.domain.oauth.client.OAuthAppleClient;
import com.semosan.api.domain.oauth.client.OAuthKakaoClient;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private OAuthKakaoClient oAuthKakaoClient;

    @Mock
    private OAuthAppleClient oAuthAppleClient;

    @Mock
    private OAuthLoginProcessor oAuthLoginProcessor;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    void kakaoLoginConvertsKakaoUserInfoToOAuthProfile() {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                12345L,
                new KakaoUserInfoResponse.KakaoAccount(
                        "kakao@example.com",
                        new KakaoUserInfoResponse.KakaoAccount.Profile("카카오", "https://example.com/profile.png")
                )
        );
        OAuthLoginResponse expected = new OAuthLoginResponse(1L, "access", "refresh", false);
        when(oAuthKakaoClient.getKakaoUserInfo("kakao-token")).thenReturn(userInfo);
        when(oAuthLoginProcessor.login(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(OAuthProvider.KAKAO),
                org.mockito.ArgumentMatchers.eq(DeviceType.ANDROID)
        )).thenReturn(expected);
        ArgumentCaptor<OAuthUserProfile> profileCaptor = ArgumentCaptor.forClass(OAuthUserProfile.class);

        OAuthLoginResponse response = oAuthService.kakaoLogin(new OAuthKakaoLoginRequest("kakao-token", DeviceType.ANDROID));

        assertThat(response).isSameAs(expected);
        verify(oAuthLoginProcessor).login(profileCaptor.capture(), org.mockito.ArgumentMatchers.eq(OAuthProvider.KAKAO), org.mockito.ArgumentMatchers.eq(DeviceType.ANDROID));
        assertThat(profileCaptor.getValue().oauthId()).isEqualTo("12345");
        assertThat(profileCaptor.getValue().email()).isEqualTo("kakao@example.com");
        assertThat(profileCaptor.getValue().name()).isEqualTo("카카오");
    }

    @Test
    void kakaoLoginAllowsMissingAccountFields() {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(12345L, null);
        OAuthLoginResponse expected = new OAuthLoginResponse(1L, "access", "refresh", false);
        when(oAuthKakaoClient.getKakaoUserInfo("kakao-token")).thenReturn(userInfo);
        when(oAuthLoginProcessor.login(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(OAuthProvider.KAKAO),
                org.mockito.ArgumentMatchers.eq(DeviceType.IOS)
        )).thenReturn(expected);
        ArgumentCaptor<OAuthUserProfile> profileCaptor = ArgumentCaptor.forClass(OAuthUserProfile.class);

        OAuthLoginResponse response = oAuthService.kakaoLogin(new OAuthKakaoLoginRequest("kakao-token", DeviceType.IOS));

        assertThat(response).isSameAs(expected);
        verify(oAuthLoginProcessor).login(profileCaptor.capture(), org.mockito.ArgumentMatchers.eq(OAuthProvider.KAKAO), org.mockito.ArgumentMatchers.eq(DeviceType.IOS));
        assertThat(profileCaptor.getValue().oauthId()).isEqualTo("12345");
        assertThat(profileCaptor.getValue().email()).isNull();
        assertThat(profileCaptor.getValue().name()).isNull();
    }

    @Test
    void appleLoginConvertsClaimsToOAuthProfile() {
        Claims claims = mock(Claims.class);
        OAuthLoginResponse expected = new OAuthLoginResponse(1L, "access", "refresh", false);
        when(oAuthAppleClient.getAppleClaims("identity-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("apple-sub");
        when(claims.get("email", String.class)).thenReturn("apple@example.com");
        when(oAuthLoginProcessor.login(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(OAuthProvider.APPLE),
                org.mockito.ArgumentMatchers.eq(DeviceType.IOS)
        )).thenReturn(expected);
        ArgumentCaptor<OAuthUserProfile> profileCaptor = ArgumentCaptor.forClass(OAuthUserProfile.class);

        OAuthLoginResponse response = oAuthService.appleLogin(new OAuthAppleLoginRequest("identity-token", "애플", DeviceType.IOS));

        assertThat(response).isSameAs(expected);
        verify(oAuthLoginProcessor).login(profileCaptor.capture(), org.mockito.ArgumentMatchers.eq(OAuthProvider.APPLE), org.mockito.ArgumentMatchers.eq(DeviceType.IOS));
        assertThat(profileCaptor.getValue().oauthId()).isEqualTo("apple-sub");
        assertThat(profileCaptor.getValue().email()).isEqualTo("apple@example.com");
        assertThat(profileCaptor.getValue().name()).isEqualTo("애플");
    }
}
