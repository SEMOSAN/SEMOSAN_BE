package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.MountainLike;
import com.semosan.api.domain.mountain.enums.Difficulty;

public record LikedMountainResponse(
        Long mountainId,
        String name,
        String address,
        Double altitude,
        Difficulty difficulty,
        String imageUrl
) {
    // 좋아요한 산 정보로 응답 DTO를 생성합니다.
    public static LikedMountainResponse from(MountainLike mountainLike) {
        Mountain mountain = mountainLike.getMountain();
        return new LikedMountainResponse(
                mountain.getId(),
                mountain.getName(),
                mountain.getAddress(),
                mountain.getAltitude(),
                mountain.getDifficulty(),
                mountain.getImageUrl()
        );
    }
}
