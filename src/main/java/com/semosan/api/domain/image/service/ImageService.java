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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

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

            uploadUrl = uploadUrl.replace(minioProperties.endpoint(), minioProperties.publicUrl());
            String imageUrl = minioProperties.publicUrl() + "/" + bucket + "/" + key;

            return new PresignedUrlResponse(uploadUrl, imageUrl);
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
