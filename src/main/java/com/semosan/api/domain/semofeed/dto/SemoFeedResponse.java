package com.semosan.api.domain.semofeed.dto;

import com.semosan.api.domain.semofeed.entity.SemoFeed;

public record SemoFeedResponse(
        Long id,
        String imageUrl,
        boolean isPublic
) {
    public static SemoFeedResponse from(SemoFeed semoFeed) {
        return new SemoFeedResponse(
                semoFeed.getId(),
                semoFeed.getImageUrl(),
                semoFeed.isPublic()
        );
    }
}
