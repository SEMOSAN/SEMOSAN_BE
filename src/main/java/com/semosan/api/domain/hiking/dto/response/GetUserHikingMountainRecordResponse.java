package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingMountainRecordProjection;

import java.time.LocalDate;

public record GetUserHikingMountainRecordResponse(
        Long mountainId,
        String mountainName,
        String imageUrl,
        Long hikingCount,
        LocalDate lastHikedAt
) {

    // 조회 projection을 API 응답 DTO로 변환합니다.
    public static GetUserHikingMountainRecordResponse from(UserHikingMountainRecordProjection projection) {
        return new GetUserHikingMountainRecordResponse(
                projection.getMountainId(),
                projection.getMountainName(),
                projection.getImageUrl(),
                projection.getHikingCount(),
                projection.getLastHikedAt().toLocalDate()
        );
    }
}
