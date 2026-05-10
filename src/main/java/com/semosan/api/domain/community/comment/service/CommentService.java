package com.semosan.api.domain.community.comment.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public Comment create(Long postId, Long authorId, String content) {
        Post post = findPostOrThrow(postId);
        User author = findUserOrThrow(authorId);

        Comment comment = Comment.create(post, author, content);
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment reply(Long postId, Long authorId, Long parentId, Long mentionedUserId, String content) {
        Post post = findPostOrThrow(postId);
        User author = findUserOrThrow(authorId);
        Comment requestedParent = findActiveCommentOrThrow(parentId);

        if (!requestedParent.getPost().getId().equals(postId)) {
            throw new GeneralException(ErrorStatus.COMMENT_PARENT_POST_MISMATCH);
        }

        // 대댓글에 답글을 달면 트리 깊이를 2로 유지하기 위해 1뎁스 댓글로 정규화
        Comment actualParent = requestedParent.isReply() ? requestedParent.getParent() : requestedParent;

        User mentionedUser = mentionedUserId != null ? findUserOrThrow(mentionedUserId) : null;

        Comment reply = Comment.reply(post, author, actualParent, mentionedUser, content);
        return commentRepository.save(reply);
    }

    public Page<Comment> getCommentsByPost(Long postId, Pageable pageable) {
        Post post = findPostOrThrow(postId);
        return commentRepository.findByPostAndParentIsNullAndDeletedFalse(post, pageable);
    }

    public List<Comment> getReplies(Long parentId) {
        Comment parent = findActiveCommentOrThrow(parentId);
        return commentRepository.findByParentAndDeletedFalseOrderByCreatedAtAsc(parent);
    }

    @Transactional
    public void delete(Long commentId, Long requesterId) {
        Comment comment = findActiveCommentOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(requesterId)) {
            throw new GeneralException(ErrorStatus.COMMENT_FORBIDDEN);
        }
        comment.softDelete();
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private Comment findActiveCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new GeneralException(ErrorStatus.COMMENT_DELETED);
        }
        return comment;
    }
}
