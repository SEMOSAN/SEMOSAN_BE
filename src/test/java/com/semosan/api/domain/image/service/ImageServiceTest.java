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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        assertThat(response.contentType()).isEqualTo("image/png");
    }

    // Content-Type 이 서명에 포함되어야 이미지가 아닌 파일 업로드를 막을 수 있다.
    @Test
    void generatePresignedUrlSignsContentTypeHeader() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio:9000/posts/generated.jpg?signature=abc");

        imageService.generatePresignedUrl("posts", "photo.jpg");

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());
        assertThat(captor.getValue().extraHeaders().get("Content-Type")).containsExactly("image/jpeg");
    }

    @Test
    void generatePresignedUrlMapsExtensionToContentTypeCaseInsensitively() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio:9000/posts/generated.JPEG?signature=abc");

        PresignedUrlResponse response = imageService.generatePresignedUrl("posts", "photo.JPEG");

        assertThat(response.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void generatePresignedUrlThrowsWhenBucketIsNotAllowed() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl("app-config", "photo.png"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.INVALID_IMAGE_BUCKET);
    }

    @Test
    void generatePresignedUrlThrowsWhenBucketIsNull() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl(null, "photo.png"))
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
    void generatePresignedUrlThrowsWhenFilenameIsNull() {
        assertThatThrownBy(() -> imageService.generatePresignedUrl("posts", null))
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
