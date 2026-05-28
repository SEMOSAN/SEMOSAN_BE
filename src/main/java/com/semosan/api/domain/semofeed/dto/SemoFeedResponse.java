package com.semosan.api.domain.semofeed.dto;

import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;

import java.util.Map;

public record SemoFeedResponse(
        Long id,
        Long userId,
        String profileUrl,
        String nickname,
        String imageUrl,
        boolean isPublic,
        Map<SemoFeedEmojiType, Long> emojiCounts,
        Map<SemoFeedEmojiType, Boolean> reactedByMe,
        boolean mine
) {
    public static SemoFeedResponse from(SemoFeed semoFeed) {
        return of(
                semoFeed,
                Map.of(),
                Map.of(),
                false
        );
    }

    public static SemoFeedResponse of(
            SemoFeed semoFeed,
            Map<SemoFeedEmojiType, Long> emojiCounts,
            Map<SemoFeedEmojiType, Boolean> reactedByMe,
            boolean mine
    ) {
        return new SemoFeedResponse(
                semoFeed.getId(),
                semoFeed.getUser().getId(),
                semoFeed.getUser().getProfileUrl(),
                semoFeed.getUser().displayName(),
                semoFeed.getImageUrl(),
                semoFeed.isPublic(),
                emojiCounts,
                reactedByMe,
                mine
        );
    }
}
