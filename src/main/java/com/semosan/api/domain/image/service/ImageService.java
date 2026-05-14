package com.semosan.api.domain.image.service;

import com.semosan.api.common.config.MinioProperties;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ImageService {

    public static final Set<String> ALLOWED_BUCKETS = Set.of("reviews", "mountains", "restaurants", "posts", "semofeed");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp"
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public PresignedUrlResponse generatePresignedUrl(String bucket, String filename) {
        validateBucket(bucket);
        String extension = extractExtension(filename);
        validateExtension(extension);

        String key = UUID.randomUUID() + extension;

        try {
            String contentType = CONTENT_TYPE_MAP.get(extension.toLowerCase());
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .expiry(10, TimeUnit.MINUTES)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build()
            );

            uploadUrl = uploadUrl.replace(minioProperties.endpoint(), minioProperties.publicUrl());
            String imageUrl = minioProperties.publicUrl() + "/" + bucket + "/" + key;

            return new PresignedUrlResponse(uploadUrl, imageUrl);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateBucket(String bucket) {
        if (bucket == null || !ALLOWED_BUCKETS.contains(bucket)) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_BUCKET);
        }
    }

    private void validateExtension(String extension) {
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
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
