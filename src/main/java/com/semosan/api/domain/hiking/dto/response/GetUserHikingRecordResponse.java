package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record GetUserHikingRecordResponse(
        Long hikingRecordId,
        Long sessionId,
        Long mountainId,
        String mountainName,
        Long courseId,
        String courseName,
        /** 화면에 표시할 기록 이름. 사용자가 이름을 정하지 않은 코스 기록은 코스명으로 채워진다. */
        String recordName,
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
                projection.getSessionId(),
                projection.getMountainId(),
                projection.getMountainName(),
                projection.getCourseId(),
                projection.getCourseName(),
                resolveRecordName(projection.getRecordName(), projection.getCourseName()),
                buildImageUrls(projection.getPhotoReportImageUrl(), projection.getCliveImageUrl()),
                projection.getDistance(),
                projection.getDuration(),
                projection.getHikedAt().toLocalDate()
        );
    }

    // 사용자가 이름을 정하지 않은 코스 기록은 코스명으로 대체합니다.
    private static String resolveRecordName(String recordName, String courseName) {
        return recordName != null ? recordName : courseName;
    }

    // 사진 URL 필드 중 존재하는 값만 응답 순서에 맞춰 모읍니다.
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
