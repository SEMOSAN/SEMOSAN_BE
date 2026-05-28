package com.semosan.api.domain.semofeed.dto;

import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;

public record SemoFeedEmojiToggleResponse(
        SemoFeedEmojiType emojiType,
        boolean reacted,
        long count
) {
}
