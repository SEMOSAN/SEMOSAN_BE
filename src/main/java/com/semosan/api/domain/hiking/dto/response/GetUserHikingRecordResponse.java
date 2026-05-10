package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;

import java.time.LocalDate;

public record GetUserHikingRecordResponse(
        Long mountainId,
        String mountainName,
        String imageUrl,
        Long hikingCount,
        LocalDate lastHikedAt
) {

    // 조회 projection을 API 응답 DTO로 변환합니다.
    public static GetUserHikingRecordResponse from(UserHikingRecordProjection projection) {
        return new GetUserHikingRecordResponse(
                projection.getMountainId(),
                projection.getMountainName(),
                projection.getImageUrl(),
                projection.getHikingCount(),
                projection.getLastHikedAt().toLocalDate()
        );
    }
}
