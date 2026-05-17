package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;

import java.util.List;

public record MountainListResponse(
        Long mountainId,
        String name,
        String address,
        Double altitude,
        Difficulty difficulty,
        Integer duration,
        List<String> imageUrls,
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
                mountain.getImageUrls(),
                mountain.getLatitude(),
                mountain.getLongitude()
        );
    }
}
