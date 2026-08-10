package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.entity.FreePost;

import java.time.LocalDateTime;

public record FreePostListResponse(
        Long id,
        AuthorResponse author,
        String title,
        String contentPreview,
        String thumbnailUrl,
        Integer extraImageCount,
        Long likeCount,
        Long commentCount,
        LocalDateTime createdAt
) {
    private static final int PREVIEW_LENGTH = 100;

    public static FreePostListResponse of(
            FreePost post,
            String thumbnailUrl,
            Integer extraImageCount,
            long likeCount,
            long commentCount
    ) {
        return new FreePostListResponse(
                post.getId(),
                AuthorResponse.from(post.getAuthor()),
                post.getTitle(),
                preview(post.getContent()),
                thumbnailUrl,
                extraImageCount != null && extraImageCount > 1 ? extraImageCount - 1 : null,
                likeCount,
                commentCount,
                post.getCreatedAt()
        );
    }

    private static String preview(String content) {
        if (content == null) return null;
        return content.length() > PREVIEW_LENGTH
                ? content.substring(0, PREVIEW_LENGTH)
                : content;
    }
}
