package com.semosan.api.domain.oauth.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.oauth.service.OAuthService;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock
    private OAuthService oAuthService;

    @InjectMocks
    private OAuthController oAuthController;

    @Test
    void kakaoLoginReturnsSuccessResponse() {
        OAuthKakaoLoginRequest request = new OAuthKakaoLoginRequest("kakao-token", DeviceType.IOS);
        OAuthLoginResponse loginResponse = new OAuthLoginResponse(1L, "access", "refresh", true);
        when(oAuthService.kakaoLogin(request)).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<OAuthLoginResponse>> response = oAuthController.kakaoLogin(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(loginResponse);
        verify(oAuthService).kakaoLogin(request);
    }

    @Test
    void appleLoginReturnsSuccessResponse() {
        OAuthAppleLoginRequest request = new OAuthAppleLoginRequest("identity-token", "애플", DeviceType.ANDROID);
        OAuthLoginResponse loginResponse = new OAuthLoginResponse(1L, "access", "refresh", false);
        when(oAuthService.appleLogin(request)).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<OAuthLoginResponse>> response = oAuthController.appleLogin(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(loginResponse);
        verify(oAuthService).appleLogin(request);
    }
}
