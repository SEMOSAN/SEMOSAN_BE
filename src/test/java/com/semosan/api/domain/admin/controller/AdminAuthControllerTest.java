package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.dto.request.AdminLoginRequest;
import com.semosan.api.domain.admin.dto.response.AdminLoginResponse;
import com.semosan.api.domain.admin.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @InjectMocks
    private AdminAuthController adminAuthController;

    @Test
    void loginPassesClientMetadataAndReturnsSuccessResponse() {
        AdminLoginRequest request = new AdminLoginRequest("admin", "password");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        AdminLoginResponse loginResponse = new AdminLoginResponse(1L, "관리자", "access");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(adminAuthService.login(request, "127.0.0.1", "JUnit")).thenReturn(loginResponse);

        ResponseEntity<ApiResponse<AdminLoginResponse>> response = adminAuthController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(loginResponse);
        verify(adminAuthService).login(request, "127.0.0.1", "JUnit");
    }
}
