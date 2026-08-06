package com.semosan.api.domain.image.service;

import com.semosan.api.common.config.MinioProperties;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private MinioClient minioClient;

    private final MinioProperties minioProperties = new MinioProperties(
            "http://minio:9000",
            "access",
            "secret",
            "https://cdn.example.com"
    );

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(minioClient, minioProperties);
    }

    @Test
    void generatePresignedUrlReturnsPublicUrls() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio:9000/posts/generated.png?signature=abc");

        PresignedUrlResponse response = imageService.generatePresignedUrl("posts", "photo.PNG");

        assertThat(response.uploadUrl()).startsWith("https://cdn.example.com/posts/");
        assertThat(response.uploadUrl()).endsWith("?signature=abc");
        assertThat(response.imageUrl()).startsWith("https://cdn.example.com/posts/");
        assertThat(response.imageUrl()).endsWith(".PNG");
    }

    @Test
    void generatePresignedUrlThrowsWhenBucketIsNotAllowed() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl("app-config", "photo.png"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.INVALID_IMAGE_BUCKET);
    }

    @Test
    void generatePresignedUrlThrowsWhenExtensionIsNotAllowed() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl("posts", "photo.gif"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.INVALID_IMAGE_EXTENSION);
    }

    @Test
    void generatePresignedUrlThrowsWhenFilenameHasNoExtension() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl("posts", "photo"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.INVALID_IMAGE_EXTENSION);
    }

    @Test
    void generatePresignedUrlThrowsWhenMinioFails() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        assertThatThrownBy(() -> imageService.generatePresignedUrl("posts", "photo.png"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.IMAGE_UPLOAD_FAILED);
    }
}
