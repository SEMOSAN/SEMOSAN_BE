package com.semosan.api.domain.tracking.dto.message;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 프론트 → 서버 (STOMP SEND /app/tracking/{sessionId}/gps).
 * 좌표만 받는다 — 리버스 지오코딩 결과는 받지 않음.
 */
public record GpsPointMessage(
        @NotNull(message = "위도(lat)는 필수입니다.")
        Double lat,

        @NotNull(message = "경도(lng)는 필수입니다.")
        Double lng,

        /** 디바이스가 보고한 고도(m). 일부 디바이스에서 누락 가능. */
        Double altitude,

        @NotNull(message = "측정 시각(recordedAt)은 필수입니다.")
        LocalDateTime recordedAt
) {
}
