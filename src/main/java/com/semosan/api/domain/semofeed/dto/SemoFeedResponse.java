package com.semosan.api.domain.semofeed.dto;

import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;

import java.util.EnumMap;
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
                defaultEmojiCounts(),
                defaultReactedByMe(),
                true
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

    // 새로 생성된 세모피드 응답에 기본 이모지 개수를 채웁니다.
    private static Map<SemoFeedEmojiType, Long> defaultEmojiCounts() {
        Map<SemoFeedEmojiType, Long> emojiCounts = new EnumMap<>(SemoFeedEmojiType.class);
        for (SemoFeedEmojiType emojiType : SemoFeedEmojiType.values()) {
            emojiCounts.put(emojiType, 0L);
        }
        return emojiCounts;
    }

    // 새로 생성된 세모피드 응답에 기본 내 반응 상태를 채웁니다.
    private static Map<SemoFeedEmojiType, Boolean> defaultReactedByMe() {
        Map<SemoFeedEmojiType, Boolean> reactedByMe = new EnumMap<>(SemoFeedEmojiType.class);
        for (SemoFeedEmojiType emojiType : SemoFeedEmojiType.values()) {
            reactedByMe.put(emojiType, false);
        }
        return reactedByMe;
    }
}
