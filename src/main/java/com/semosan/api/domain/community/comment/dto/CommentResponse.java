package com.semosan.api.domain.community.comment.dto;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.dto.AuthorResponse;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        AuthorResponse author,
        String content,
        Long parentId,
        AuthorResponse mentionedUser,
        LocalDateTime createdAt,
        boolean isDeleted
) {
    private static final String DELETED_CONTENT = "삭제된 댓글입니다";

    public static CommentResponse from(Comment comment) {
        boolean deleted = comment.isDeleted();
        return new CommentResponse(
                comment.getId(),
                AuthorResponse.from(comment.getAuthor()),
                deleted ? DELETED_CONTENT : comment.getContent(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getMentionedUser() != null ? AuthorResponse.from(comment.getMentionedUser()) : null,
                comment.getCreatedAt(),
                deleted
        );
    }
}
