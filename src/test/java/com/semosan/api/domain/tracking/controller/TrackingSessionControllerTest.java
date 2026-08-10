package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.dto.request.CompleteTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingRestoreResponse;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import com.semosan.api.domain.tracking.dto.response.TrackingTrackResponse;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.service.TrackingSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingSessionControllerTest {

    @Mock
    private TrackingSessionService trackingSessionService;

    @InjectMocks
    private TrackingSessionController trackingSessionController;

    @Test
    void createSessionReturnsSuccessResponse() {
        CreateTrackingSessionRequest request = new CreateTrackingSessionRequest(10L, null, true);
        TrackingSessionResponse session = session(100L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionService.create(1L, request)).thenReturn(session);

        ResponseEntity<ApiResponse<TrackingSessionResponse>> response =
                trackingSessionController.createSession(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_SESSION_CREATE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(session);
    }

    @Test
    void getActiveSessionReturnsNullDataWhenActiveSessionMissing() {
        when(trackingSessionService.getActive(1L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<TrackingSessionResponse>> response =
                trackingSessionController.getActiveSession(1L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_SESSION_GET_ACTIVE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void getSessionReturnsSuccessResponse() {
        TrackingSessionResponse session = session(100L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionService.get(1L, 100L)).thenReturn(session);

        ResponseEntity<ApiResponse<TrackingSessionResponse>> response = trackingSessionController.getSession(1L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.TRACKING_SESSION_GET_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(session);
    }

    @Test
    void pauseResumeCompleteAndAbandonDelegateToService() {
        TrackingSessionResponse paused = session(100L, TrackingSessionStatus.PAUSED);
        TrackingSessionResponse inProgress = session(100L, TrackingSessionStatus.IN_PROGRESS);
        TrackingSessionResponse completed = session(100L, TrackingSessionStatus.COMPLETED);
        TrackingSessionResponse abandoned = session(100L, TrackingSessionStatus.ABANDONED);
        when(trackingSessionService.pause(1L, 100L)).thenReturn(paused);
        when(trackingSessionService.resume(1L, 100L)).thenReturn(inProgress);
        when(trackingSessionService.complete(1L, 100L, null)).thenReturn(completed);
        when(trackingSessionService.abandon(1L, 100L)).thenReturn(abandoned);

        assertThat(trackingSessionController.pauseSession(1L, 100L).getBody().getData()).isSameAs(paused);
        assertThat(trackingSessionController.resumeSession(1L, 100L).getBody().getData()).isSameAs(inProgress);
        assertThat(trackingSessionController.completeSession(1L, 100L, null).getBody().getData()).isSameAs(completed);
        assertThat(trackingSessionController.abandonSession(1L, 100L).getBody().getData()).isSameAs(abandoned);
        verify(trackingSessionService).pause(1L, 100L);
        verify(trackingSessionService).resume(1L, 100L);
        verify(trackingSessionService).complete(1L, 100L, null);
        verify(trackingSessionService).abandon(1L, 100L);
    }

    @Test
    void completeSessionPassesRequestedNameToService() {
        TrackingSessionResponse completed = session(100L, TrackingSessionStatus.COMPLETED);
        when(trackingSessionService.complete(1L, 100L, "북한산 아침 산책")).thenReturn(completed);

        ResponseEntity<ApiResponse<TrackingSessionResponse>> response = trackingSessionController
                .completeSession(1L, 100L, new CompleteTrackingSessionRequest("북한산 아침 산책"));

        assertThat(response.getBody().getData()).isSameAs(completed);
        verify(trackingSessionService).complete(1L, 100L, "북한산 아침 산책");
    }

    @Test
    void completeSessionPassesNullWhenBodyIsOmitted() {
        // body 를 생략하고 호출하는 경우 — 서버가 기본 이름을 채우도록 null 을 그대로 넘긴다.
        TrackingSessionResponse completed = session(100L, TrackingSessionStatus.COMPLETED);
        when(trackingSessionService.complete(1L, 100L, null)).thenReturn(completed);

        trackingSessionController.completeSession(1L, 100L, null);

        verify(trackingSessionService).complete(1L, 100L, null);
    }

    @Test
    void getSessionTrackReturnsSuccessResponse() {
        TrackingTrackResponse track = TrackingTrackResponse.of(
                100L, "{\"type\":\"LineString\",\"coordinates\":[]}", "[310.0]");
        when(trackingSessionService.getTrack(1L, 100L)).thenReturn(track);

        ResponseEntity<ApiResponse<TrackingTrackResponse>> response =
                trackingSessionController.getSessionTrack(1L, 100L);

        assertThat(response.getStatusCode())
                .isEqualTo(SuccessStatus.TRACKING_SESSION_GET_TRACK_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(track);
        verify(trackingSessionService).getTrack(1L, 100L);
    }

    @Test
    void restoreSessionReturnsSuccessResponse() {
        TrackingRestoreResponse restore = new TrackingRestoreResponse(
                session(100L, TrackingSessionStatus.IN_PROGRESS),
                4_820L,
                new TrackingRestoreResponse.Stats(3241.7, 452.0, 88.3, 781.2, 842L),
                new TrackingRestoreResponse.PhotoMilestone(
                        List.of(812.5), List.of(0), List.of(), false)
        );
        when(trackingSessionService.restore(1L, 100L)).thenReturn(restore);

        ResponseEntity<ApiResponse<TrackingRestoreResponse>> response =
                trackingSessionController.restoreSession(1L, 100L);

        assertThat(response.getStatusCode())
                .isEqualTo(SuccessStatus.TRACKING_SESSION_RESTORE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(restore);
        verify(trackingSessionService).restore(1L, 100L);
    }

    private TrackingSessionResponse session(Long sessionId, TrackingSessionStatus status) {
        return new TrackingSessionResponse(
                sessionId, 1L, 10L, "관악산", null, null, true, status,
                LocalDateTime.of(2026, 8, 6, 10, 0), null, null, 0, null
        );
    }
}
