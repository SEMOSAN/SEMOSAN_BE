package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection;

public record GetUserHikingRecordSummaryResponse(
        Long totalHikingCount,
        Long conqueredMountainCount,
        Double totalAltitude
) {

    // 조회 projection을 등산 기록 요약 응답 DTO로 변환합니다.
    public static GetUserHikingRecordSummaryResponse from(UserHikingRecordSummaryProjection projection) {
        return new GetUserHikingRecordSummaryResponse(
                projection.getTotalHikingCount(),
                projection.getConqueredMountainCount(),
                projection.getTotalAltitude()
        );
    }

    // 등산 기록이 없는 유저의 빈 요약 응답을 생성합니다.
    public static GetUserHikingRecordSummaryResponse empty() {
        return new GetUserHikingRecordSummaryResponse(0L, 0L, 0.0);
    }
}
