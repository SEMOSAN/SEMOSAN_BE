package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.MountainLike;
import com.semosan.api.domain.mountain.enums.Difficulty;

import java.util.List;

public record LikedMountainResponse(
        Long mountainId,
        String name,
        String address,
        Double altitude,
        Difficulty difficulty,
        List<String> imageUrls
) {
    public static LikedMountainResponse from(MountainLike mountainLike) {
        Mountain mountain = mountainLike.getMountain();
        return new LikedMountainResponse(
                mountain.getId(),
                mountain.getName(),
                mountain.getAddress(),
                mountain.getAltitude(),
                mountain.getDifficulty(),
                mountain.getImageUrls()
        );
    }
}
