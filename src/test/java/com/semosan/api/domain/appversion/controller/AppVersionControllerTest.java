package com.semosan.api.domain.appversion.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.appversion.dto.request.UpdateAppVersionRequest;
import com.semosan.api.domain.appversion.dto.response.AppVersionResponse;
import com.semosan.api.domain.appversion.service.AppVersionService;
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
class AppVersionControllerTest {

    @Mock
    private AppVersionService appVersionService;

    @InjectMocks
    private AppVersionController appVersionController;

    @Test
    void getAppVersionReturnsSuccessResponse() {
        AppVersionResponse appVersion = appVersion();
        when(appVersionService.getAppVersion()).thenReturn(appVersion);

        ResponseEntity<ApiResponse<AppVersionResponse>> response = appVersionController.getAppVersion();

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.APP_VERSION_GET_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(appVersion);
    }

    @Test
    void updateAppVersionReturnsSuccessResponse() {
        UpdateAppVersionRequest request = request();
        AppVersionResponse appVersion = appVersion();
        when(appVersionService.updateAppVersion(request)).thenReturn(appVersion);

        ResponseEntity<ApiResponse<AppVersionResponse>> response = appVersionController.updateAppVersion(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.APP_VERSION_UPDATE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(appVersion);
        verify(appVersionService).updateAppVersion(request);
    }

    private UpdateAppVersionRequest request() {
        return new UpdateAppVersionRequest(platformRequest(), platformRequest());
    }

    private UpdateAppVersionRequest.PlatformVersionRequest platformRequest() {
        return new UpdateAppVersionRequest.PlatformVersionRequest(
                "2.0.0",
                "1.0.0",
                false,
                "https://store.example.com",
                "notes",
                false,
                null
        );
    }

    private AppVersionResponse appVersion() {
        AppVersionResponse.PlatformVersion platform = new AppVersionResponse.PlatformVersion(
                "2.0.0",
                "1.0.0",
                false,
                "https://store.example.com",
                "notes",
                false,
                null
        );
        return new AppVersionResponse(platform, platform, "2026-08-06T00:00:00Z");
    }
}
