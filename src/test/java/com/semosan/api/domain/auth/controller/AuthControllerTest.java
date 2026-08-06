package com.semosan.api.domain.auth.controller;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.auth.dto.response.ReissueResponse;
import com.semosan.api.domain.auth.service.AuthService;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginReturnsSuccessResponse() {
        LoginRequest request = new LoginRequest("test-user", DeviceType.IOS, "secret");
        LoginResponse loginResponse = new LoginResponse(1L, "access", "refresh", false);
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(loginResponse);
    }

    @Test
    void reissueExtractsBearerTokenAndReturnsSuccessResponse() {
        ReissueResponse reissueResponse = new ReissueResponse("new-access", "new-refresh");
        when(authService.reissue("refresh-token")).thenReturn(reissueResponse);

        ResponseEntity<ApiResponse<ReissueResponse>> response = authController.reissue("Bearer refresh-token");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.REISSUE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(reissueResponse);
        verify(authService).reissue("refresh-token");
    }

    @Test
    void logoutExtractsBearerTokenAndDelegates() {
        ResponseEntity<ApiResponse<Void>> response = authController.logout(1L, "Bearer access-token");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGOUT_SUCCESS.getHttpStatus());
        verify(authService).logout(1L, "access-token");
    }

    @Test
    void withdrawExtractsBearerTokenAndDelegates() {
        ResponseEntity<ApiResponse<Void>> response = authController.withdraw(1L, "Bearer access-token");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.WITHDRAW_SUCCESS.getHttpStatus());
        verify(authService).withdraw(1L, "access-token");
    }

    @Test
    void reissueThrowsWhenAuthorizationHeaderIsInvalid() {
        assertThatThrownBy(() -> authController.reissue("refresh-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_TOKEN_NOT_FOUND);
    }
}
