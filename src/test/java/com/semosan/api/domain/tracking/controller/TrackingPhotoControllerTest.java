package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.dto.request.TrackingPhotoUploadRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingPhotoResponse;
import com.semosan.api.domain.tracking.service.TrackingPhotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPhotoControllerTest {

    @Mock
    private TrackingPhotoService trackingPhotoService;

    @InjectMocks
    private TrackingPhotoController trackingPhotoController;

    @Test
    void uploadReturnsSuccessResponse() {
        TrackingPhotoUploadRequest request = request();
        TrackingPhotoResponse photo = photo();
        when(trackingPhotoService.upload(1L, 100L, request)).thenReturn(photo);

        ResponseEntity<ApiResponse<TrackingPhotoResponse>> response =
                trackingPhotoController.upload(1L, 100L, request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_PHOTO_UPLOAD_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(photo);
    }

    @Test
    void listReturnsSuccessResponse() {
        TrackingPhotoResponse photo = photo();
        when(trackingPhotoService.listBySession(1L, 100L)).thenReturn(List.of(photo));

        ResponseEntity<ApiResponse<List<TrackingPhotoResponse>>> response =
                trackingPhotoController.list(1L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_PHOTO_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).containsExactly(photo);
        verify(trackingPhotoService).listBySession(1L, 100L);
    }

    private TrackingPhotoUploadRequest request() {
        return new TrackingPhotoUploadRequest(
                0, 500.0, "image.jpg", LocalDateTime.of(2026, 8, 6, 10, 0), 37.5, 127.0, 123.4
        );
    }

    private TrackingPhotoResponse photo() {
        return new TrackingPhotoResponse(
                1L, 100L, 0, 500.0, "image.jpg",
                LocalDateTime.of(2026, 8, 6, 10, 0), 37.5, 127.0, 123.4
        );
    }
}
