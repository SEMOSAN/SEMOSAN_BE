package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.entity.PostImage;

public record PostImageResponse(
        Long id,
        String imageUrl,
        Integer sortOrder,
        boolean main
) {
    public static PostImageResponse from(PostImage image) {
        return new PostImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getSortOrder(),
                image.isMain()
        );
    }
}
