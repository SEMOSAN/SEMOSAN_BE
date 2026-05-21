package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;

public record MountainRecommendationResponse(
        Long mountainId,
        String name,
        String imageUrl,
        Difficulty difficulty,
        Double altitude,
        String address
) {

    public static MountainRecommendationResponse from(Mountain mountain) {
        return new MountainRecommendationResponse(
                mountain.getId(),
                mountain.getName(),
                firstImageUrl(mountain),
                mountain.getDifficulty(),
                mountain.getAltitude(),
                mountain.getAddress()
        );
    }

    private static String firstImageUrl(Mountain mountain) {
        return mountain.getImageUrls() == null || mountain.getImageUrls().isEmpty()
                ? null
                : mountain.getImageUrls().get(0);
    }
}
