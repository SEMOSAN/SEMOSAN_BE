package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record GetUserHikingRecordResponse(
        Long hikingRecordId,
        Long mountainId,
        String mountainName,
        Long courseId,
        String courseName,
        List<String> imageUrls,
        Double distance,
        Integer duration,
        LocalDate hikedAt
) {

    // 조회 projection을 나의 등산 기록 상세 응답 DTO로 변환합니다.
    // imageUrls 순서: photoReport(앞)이 cliveImage(뒤)보다 먼저. null인 필드는 제외.
    public static GetUserHikingRecordResponse from(UserHikingRecordProjection projection) {
        return new GetUserHikingRecordResponse(
                projection.getHikingRecordId(),
                projection.getMountainId(),
                projection.getMountainName(),
                projection.getCourseId(),
                projection.getCourseName(),
                buildImageUrls(projection.getPhotoReportImageUrl(), projection.getCliveImageUrl()),
                projection.getDistance(),
                projection.getDuration(),
                projection.getHikedAt().toLocalDate()
        );
    }

    private static List<String> buildImageUrls(String photoReportImageUrl, String cliveImageUrl) {
        List<String> urls = new ArrayList<>(2);
        if (photoReportImageUrl != null) {
            urls.add(photoReportImageUrl);
        }
        if (cliveImageUrl != null) {
            urls.add(cliveImageUrl);
        }
        return urls;
    }
}
