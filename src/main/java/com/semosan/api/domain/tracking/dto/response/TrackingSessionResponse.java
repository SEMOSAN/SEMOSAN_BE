package com.semosan.api.domain.tracking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;

import java.time.LocalDateTime;

public record TrackingSessionResponse(
        Long sessionId,
        Long userId,
        Long mountainId,
        String mountainName,
        Long courseId,
        String courseName,
        Boolean isFreeRecording,
        TrackingSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime pausedAt,
        Integer pausedSecondsTotal,
        /** complete 시에만 채워짐 — 변환된 HikingRecord 의 ID. 다른 경로에선 null 직렬화에서 제외. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long hikingRecordId
) {

    public static TrackingSessionResponse from(TrackingSession session) {
        return from(session, null);
    }

    public static TrackingSessionResponse from(TrackingSession session, Long hikingRecordId) {
        return new TrackingSessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getMountain().getId(),
                session.getMountain().getName(),
                session.getCourse() != null ? session.getCourse().getId() : null,
                session.getCourse() != null ? session.getCourse().getName() : null,
                session.getIsFreeRecording(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getPausedAt(),
                session.getPausedSecondsTotal(),
                hikingRecordId
        );
    }
}
