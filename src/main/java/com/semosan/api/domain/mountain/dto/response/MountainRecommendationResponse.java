package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.FitnessLevel;
import com.semosan.api.domain.mountain.service.recommendation.TrackScorer.TrackMetrics;

public record MountainRecommendationResponse(
        Long mountainId,
        String name,
        String imageUrl,
        String difficultyLabel,
        Integer mountainHeightM,
        String address
) {

    public static MountainRecommendationResponse from(
            Mountain mountain,
            TrackMetrics metrics,
            FitnessLevel fitnessLevel
    ) {
        return new MountainRecommendationResponse(
                mountain.getId(),
                mountain.getName(),
                firstImageUrl(mountain),
                difficultyLabel(mountain.getDifficulty(), fitnessLevel),
                metrics.mountainHeightM(),
                mountain.getAddress()
        );
    }

    private static String difficultyLabel(Difficulty difficulty, FitnessLevel fitnessLevel) {
        if (difficulty == null) {
            return fitnessLevel.getLabel();
        }
        return switch (difficulty) {
            case EASY -> "초";
            case NORMAL -> "중";
            case HARD -> "상";
        };
    }

    private static String firstImageUrl(Mountain mountain) {
        return mountain.getImageUrls() == null || mountain.getImageUrls().isEmpty()
                ? null
                : mountain.getImageUrls().get(0);
    }
}
