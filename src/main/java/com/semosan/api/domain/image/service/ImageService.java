package com.semosan.api.domain.image.service;

import com.semosan.api.common.config.MinioProperties;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.constant.MinioConstants;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 업로드용 presigned PUT URL 발급.
     *
     * 확장자로 결정한 Content-Type 을 서명에 포함시켜, 발급받은 URL 로 이미지가 아닌 파일을
     * 올리지 못하게 막는다. 클라이언트는 응답의 contentType 을 그대로 헤더에 실어야 한다.
     */
    public PresignedUrlResponse generatePresignedUrl(String bucket, String filename) {
        validateBucket(bucket);
        String extension = extractExtension(filename);
        validateExtension(extension);
        String contentType = MinioConstants.CONTENT_TYPE_MAP.get(extension.toLowerCase());

        String key = UUID.randomUUID() + extension;

        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .extraHeaders(Map.of(CONTENT_TYPE_HEADER, contentType))
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );

            uploadUrl = uploadUrl.replace(minioProperties.endpoint(), minioProperties.publicUrl());
            String imageUrl = minioProperties.publicUrl() + "/" + bucket + "/" + key;

            return new PresignedUrlResponse(uploadUrl, imageUrl, contentType);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateBucket(String bucket) {
        if (bucket == null || !MinioConstants.ALLOWED_IMAGE_BUCKETS.contains(bucket)) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_BUCKET);
        }
    }

    private void validateExtension(String extension) {
        if (extension.isEmpty() || !MinioConstants.ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_EXTENSION);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
