package com.semosan.api.domain.appversion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.constant.MinioConstants;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.appversion.dto.request.UpdateAppVersionRequest;
import com.semosan.api.domain.appversion.dto.response.AppVersionResponse;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class AppVersionService {

    private static final String BUCKET = MinioConstants.APP_CONFIG_BUCKET;
    private static final String OBJECT_KEY = "app-version.json";

    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    public AppVersionResponse getAppVersion() {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(OBJECT_KEY)
                        .build()
        )) {
            return objectMapper.readValue(stream, AppVersionResponse.class);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new GeneralException(ErrorStatus.APP_VERSION_NOT_FOUND);
            }
            throw new GeneralException(ErrorStatus.APP_VERSION_READ_FAILED);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.APP_VERSION_READ_FAILED);
        }
    }

    public AppVersionResponse updateAppVersion(UpdateAppVersionRequest request) {
        AppVersionResponse response = new AppVersionResponse(
                new AppVersionResponse.PlatformVersion(
                        request.ios().latestVersion(),
                        request.ios().minimumVersion(),
                        request.ios().forceUpdate(),
                        request.ios().storeUrl(),
                        request.ios().releaseNotes(),
                        request.ios().maintenanceMode(),
                        request.ios().maintenanceMessage()
                ),
                new AppVersionResponse.PlatformVersion(
                        request.android().latestVersion(),
                        request.android().minimumVersion(),
                        request.android().forceUpdate(),
                        request.android().storeUrl(),
                        request.android().releaseNotes(),
                        request.android().maintenanceMode(),
                        request.android().maintenanceMessage()
                ),
                java.time.Instant.now().toString()
        );

        try {
            byte[] json = objectMapper.writeValueAsBytes(response);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(json);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(OBJECT_KEY)
                            .stream(inputStream, json.length, -1)
                            .contentType("application/json")
                            .build()
            );

            return response;
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.APP_VERSION_UPDATE_FAILED);
        }
    }
}