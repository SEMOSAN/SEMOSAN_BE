package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.dto.response.LiveActivityCourseResponse;
import com.semosan.api.domain.tracking.dto.response.NearbyMountainResponse;
import com.semosan.api.domain.tracking.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    @Mock
    private TrackingService trackingService;

    @InjectMocks
    private TrackingController trackingController;

    @Test
    void getNearbyMountainReturnsSuccessResponse() {
        NearbyMountainResponse nearby = new NearbyMountainResponse(null, List.of());
        when(trackingService.getNearbyMountain(1L, 37.5, 127.0)).thenReturn(nearby);

        ResponseEntity<ApiResponse<NearbyMountainResponse>> response =
                trackingController.getNearbyMountain(1L, 37.5, 127.0);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_NEAREST_MOUNTAIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(nearby);
    }

    @Test
    void getLiveActivityCourseReturnsSuccessResponse() {
        LiveActivityCourseResponse course = new LiveActivityCourseResponse(10L, List.of(), 1500.0, 90, 500.0, 30);
        when(trackingService.getLiveActivityCourse(1L, 10L)).thenReturn(course);

        ResponseEntity<ApiResponse<LiveActivityCourseResponse>> response =
                trackingController.getLiveActivityCourse(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_LIVE_ACTIVITY_COURSE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(course);
        verify(trackingService).getLiveActivityCourse(1L, 10L);
    }
}
