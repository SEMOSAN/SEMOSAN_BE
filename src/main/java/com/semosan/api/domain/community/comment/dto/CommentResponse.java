package com.semosan.api.domain.community.comment.dto;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.dto.AuthorResponse;

import java.time.LocalDateTime;
import java.util.Set;

public record CommentResponse(
        Long id,
        AuthorResponse author,
        String content,
        Long parentId,
        AuthorResponse mentionedUser,
        LocalDateTime createdAt,
        boolean isDeleted,
        boolean isBlocked
) {
    private static final String DELETED_CONTENT = "삭제된 댓글입니다";
    private static final String BLOCKED_CONTENT = "차단한 사용자입니다.";

    public static CommentResponse from(Comment comment) {
        return from(comment, Set.of());
    }

    public static CommentResponse from(Comment comment, Set<Long> blockedUserIds) {
        boolean deleted = comment.isDeleted();
        boolean blocked = !deleted && blockedUserIds.contains(comment.getAuthor().getId());
        String content;
        if (deleted) content = DELETED_CONTENT;
        else if (blocked) content = BLOCKED_CONTENT;
        else content = comment.getContent();

        return new CommentResponse(
                comment.getId(),
                AuthorResponse.from(comment.getAuthor()),
                content,
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getMentionedUser() != null ? AuthorResponse.from(comment.getMentionedUser()) : null,
                comment.getCreatedAt(),
                deleted,
                blocked
        );
    }
}
