package com.semosan.api.domain.admin.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;

import java.util.List;

public record AdminMountainListResponse(
        Long mountainId,
        String name,
        String address,
        Double altitude,
        Difficulty difficulty,
        Integer duration,
        List<String> imageUrls,
        Double latitude,
        Double longitude,
        boolean isPublic,
        long courseCount
) {

    public static AdminMountainListResponse from(Mountain mountain, long courseCount) {
        return new AdminMountainListResponse(
                mountain.getId(),
                mountain.getName(),
                mountain.getAddress(),
                mountain.getAltitude(),
                mountain.getDifficulty(),
                mountain.getDuration(),
                mountain.getImageUrls(),
                mountain.getLatitude(),
                mountain.getLongitude(),
                mountain.isPublic(),
                courseCount
        );
    }
}
