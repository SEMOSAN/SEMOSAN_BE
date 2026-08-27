package com.semosan.api.domain.admin.dto.response;

import com.semosan.api.domain.semofeed.entity.SemoFeed;

import java.time.LocalDateTime;

public record AdminSemoFeedResponse(
        Long semoFeedId,
        String imageUrl,
        Long authorId,
        String authorNickname,
        boolean isPublic,
        LocalDateTime createdAt
) {
    public static AdminSemoFeedResponse from(SemoFeed feed) {
        return new AdminSemoFeedResponse(
                feed.getId(),
                feed.getImageUrl(),
                feed.getUser().getId(),
                feed.getUser().getNickname(),
                feed.isPublic(),
                feed.getCreatedAt()
        );
    }
}
