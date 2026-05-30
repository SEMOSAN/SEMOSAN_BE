package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingMountainRecordProjection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record GetUserHikingMountainRecordResponse(
        Long mountainId,
        String mountainName,
        List<String> imageUrls,
        Long hikingCount,
        LocalDate lastHikedAt
) {

    // 조회 projection을 API 응답 DTO로 변환합니다.
    public static GetUserHikingMountainRecordResponse from(UserHikingMountainRecordProjection projection) {
        return new GetUserHikingMountainRecordResponse(
                projection.getMountainId(),
                projection.getMountainName(),
                buildImageUrls(projection.getImageUrl1(), projection.getImageUrl2()),
                projection.getHikingCount(),
                projection.getLastHikedAt().toLocalDate()
        );
    }

    private static List<String> buildImageUrls(String imageUrl1, String imageUrl2) {
        List<String> imageUrls = new ArrayList<>(2);
        addIfPresent(imageUrls, imageUrl1);
        addIfPresent(imageUrls, imageUrl2);
        return imageUrls;
    }

    private static void addIfPresent(List<String> imageUrls, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            imageUrls.add(imageUrl);
        }
    }
}
