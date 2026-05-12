package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;

import java.time.LocalDate;

public record GetUserHikingRecordResponse(
        Long hikingRecordId,
        Long mountainId,
        String mountainName,
        Long courseId,
        String courseName,
        String imageUrl,
        Double distance,
        Integer duration,
        LocalDate hikedAt
) {

    // 조회 projection을 나의 등산 기록 상세 응답 DTO로 변환합니다.
    public static GetUserHikingRecordResponse from(UserHikingRecordProjection projection) {
        return new GetUserHikingRecordResponse(
                projection.getHikingRecordId(),
                projection.getMountainId(),
                projection.getMountainName(),
                projection.getCourseId(),
                projection.getCourseName(),
                projection.getImageUrl(),
                projection.getDistance(),
                projection.getDuration(),
                projection.getHikedAt().toLocalDate()
        );
    }
}
