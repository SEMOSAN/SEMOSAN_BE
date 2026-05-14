package com.semosan.api.domain.image.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ImageService {

    public static final Set<String> ALLOWED_BUCKETS = Set.of("reviews", "mountains", "restaurants", "posts", "semofeed");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-url}")
    private String publicUrl;

    public PresignedUrlResponse generatePresignedUrl(String bucket, String filename) {
        validateBucket(bucket);
        String extension = extractExtension(filename);
        validateExtension(extension);

        String key = UUID.randomUUID() + extension;

        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );

            uploadUrl = uploadUrl.replace(endpoint, publicUrl);
            String imageUrl = publicUrl + "/" + bucket + "/" + key;

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
