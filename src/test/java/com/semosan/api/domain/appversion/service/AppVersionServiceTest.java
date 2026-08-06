package com.semosan.api.domain.appversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.appversion.dto.request.UpdateAppVersionRequest;
import com.semosan.api.domain.appversion.dto.response.AppVersionResponse;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppVersionServiceTest {

    @Mock
    private MinioClient minioClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppVersionService appVersionService;

    @BeforeEach
    void setUp() {
        appVersionService = new AppVersionService(minioClient, objectMapper);
    }

    @Test
    void getAppVersionReadsJsonFromMinio() throws Exception {
        String json = """
                {
                  "ios": {
                    "latestVersion": "1.2.0",
                    "minimumVersion": "1.0.0",
                    "forceUpdate": true,
                    "storeUrl": "https://apps.apple.com/app",
                    "releaseNotes": "notes",
                    "maintenanceMode": false,
                    "maintenanceMessage": null
                  },
                  "android": {
                    "latestVersion": "1.3.0",
                    "minimumVersion": "1.1.0",
                    "forceUpdate": false,
                    "storeUrl": "https://play.google.com/store/apps/details?id=app",
                    "releaseNotes": "android notes",
                    "maintenanceMode": true,
                    "maintenanceMessage": "maintenance"
                  },
                  "updatedAt": "2026-08-06T00:00:00Z"
                }
                """;
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(
                        Headers.of(),
                        "app-config",
                        "kr",
                        "app-version.json",
                        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
                ));

        AppVersionResponse response = appVersionService.getAppVersion();

        assertThat(response.ios().latestVersion()).isEqualTo("1.2.0");
        assertThat(response.ios().forceUpdate()).isTrue();
        assertThat(response.android().maintenanceMode()).isTrue();
        assertThat(response.updatedAt()).isEqualTo("2026-08-06T00:00:00Z");
    }

    @Test
    void getAppVersionThrowsReadFailedWhenMinioFails() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        assertThatThrownBy(() -> appVersionService.getAppVersion())
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APP_VERSION_READ_FAILED);
    }

    @Test
    void getAppVersionThrowsNotFoundWhenObjectDoesNotExist() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(errorResponseException("NoSuchKey"));

        assertThatThrownBy(() -> appVersionService.getAppVersion())
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APP_VERSION_NOT_FOUND);
    }

    @Test
    void getAppVersionThrowsReadFailedWhenMinioReturnsOtherErrorResponse() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(errorResponseException("AccessDenied"));

        assertThatThrownBy(() -> appVersionService.getAppVersion())
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APP_VERSION_READ_FAILED);
    }

    @Test
    void updateAppVersionBuildsResponseAndStoresJson() throws Exception {
        UpdateAppVersionRequest request = request();
        ArgumentCaptor<PutObjectArgs> argsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);

        AppVersionResponse response = appVersionService.updateAppVersion(request);

        assertThat(response.ios().latestVersion()).isEqualTo("2.0.0");
        assertThat(response.ios().forceUpdate()).isTrue();
        assertThat(response.android().minimumVersion()).isEqualTo("1.2.0");
        assertThat(response.updatedAt()).isNotBlank();
        verify(minioClient).putObject(argsCaptor.capture());
        assertThat(argsCaptor.getValue()).isNotNull();
    }

    @Test
    void updateAppVersionThrowsUpdateFailedWhenMinioFails() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        assertThatThrownBy(() -> appVersionService.updateAppVersion(request()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APP_VERSION_UPDATE_FAILED);
    }

    private UpdateAppVersionRequest request() {
        return new UpdateAppVersionRequest(
                new UpdateAppVersionRequest.PlatformVersionRequest(
                        "2.0.0",
                        "1.5.0",
                        true,
                        "https://apps.apple.com/app",
                        "ios notes",
                        false,
                        null
                ),
                new UpdateAppVersionRequest.PlatformVersionRequest(
                        "2.1.0",
                        "1.2.0",
                        false,
                        "https://play.google.com/store/apps/details?id=app",
                        "android notes",
                        true,
                        "maintenance"
                )
        );
    }

    private ErrorResponseException errorResponseException(String code) {
        ErrorResponse errorResponse = new ErrorResponse(
                code,
                "message",
                "app-config",
                "app-version.json",
                "/app-version.json",
                "request-id",
                "host-id"
        );
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost/app-version.json").build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
        return new ErrorResponseException(errorResponse, response, "http trace");
    }
}
