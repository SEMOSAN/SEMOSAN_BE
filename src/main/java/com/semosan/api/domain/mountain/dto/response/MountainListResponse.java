package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;

public record MountainListResponse(
        Long mountainId,
        String name,
        String address,
        Double altitude,
        Difficulty difficulty,
        Integer duration,
        String imageUrl,
        Double latitude,
        Double longitude
) {

    public static MountainListResponse from(Mountain mountain) {
        return new MountainListResponse(
                mountain.getId(),
                mountain.getName(),
                mountain.getAddress(),
                mountain.getAltitude(),
                mountain.getDifficulty(),
                mountain.getDuration(),
                mountain.getImageUrl(),
                mountain.getLatitude(),
                mountain.getLongitude()
        );
    }
}
