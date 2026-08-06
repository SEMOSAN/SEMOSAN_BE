package com.semosan.api.domain.image.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import com.semosan.api.domain.image.service.ImageService;
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
class ImageControllerTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageController imageController;

    @Test
    void getPresignedUrlReturnsSuccessResponse() {
        PresignedUrlResponse presignedUrl = new PresignedUrlResponse("upload-url", "image-url");
        when(imageService.generatePresignedUrl("posts", "photo.png")).thenReturn(presignedUrl);

        ResponseEntity<ApiResponse<PresignedUrlResponse>> response =
                imageController.getPresignedUrl("posts", "photo.png");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.PRESIGNED_URL_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(presignedUrl);
        verify(imageService).generatePresignedUrl("posts", "photo.png");
    }
}
