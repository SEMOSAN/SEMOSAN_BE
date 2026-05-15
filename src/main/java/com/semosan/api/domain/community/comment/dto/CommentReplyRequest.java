package com.semosan.api.domain.community.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentReplyRequest(
        @NotNull Long parentId,
        Long mentionedUserId,
        @NotBlank String content
) {
}
