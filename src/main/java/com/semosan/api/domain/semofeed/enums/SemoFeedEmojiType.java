package com.semosan.api.domain.semofeed.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SemoFeedEmojiType {
    FIRE("🔥"),
    HEART("❤️"),
    CONGRATS("🎉"),
    LAUGH("😂");

    private final String emoji;
}
