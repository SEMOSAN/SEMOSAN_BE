package com.semosan.api.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminReportedPostResponse(
        Long postId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        long reportCount,
        boolean deleted,
        LocalDateTime createdAt
) {
}
