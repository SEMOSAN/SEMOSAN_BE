package com.semosan.api.domain.semofeed.dto;

import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import jakarta.validation.constraints.NotNull;

public record SemoFeedEmojiRequest(
        @NotNull(message = "emojiType 은 필수입니다.")
        SemoFeedEmojiType emojiType
) {
}
