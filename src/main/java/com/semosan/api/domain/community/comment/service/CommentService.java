package com.semosan.api.domain.community.comment.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.dto.CommentResponse;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.notification.service.CommunityNotificationService;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserReader userReader;
    private final UserBlockRepository userBlockRepository;
    private final CommunityNotificationService communityNotificationService;

    @Transactional
    public Comment create(Long postId, Long authorId, String content) {
        Post post = findPostOrThrow(postId);
        User author = userReader.findActiveUserById(authorId);

        Comment comment = Comment.create(post, author, content);
        Comment savedComment = commentRepository.save(comment);
        communityNotificationService.sendCommentNotification(post, author, savedComment);
        return savedComment;
    }

    @Transactional
    public Comment reply(Long postId, Long authorId, Long parentId, Long mentionedUserId, String content) {
        Post post = findPostOrThrow(postId);
        User author = userReader.findActiveUserById(authorId);
        Comment requestedParent = findActiveCommentOrThrow(parentId);

        if (!requestedParent.getPost().getId().equals(postId)) {
            throw new GeneralException(ErrorStatus.COMMENT_PARENT_POST_MISMATCH);
        }

        // 대댓글에 답글을 달면 트리 깊이를 2로 유지하기 위해 1뎁스 댓글로 정규화
        Comment actualParent = requestedParent.isReply() ? requestedParent.getParent() : requestedParent;

        User mentionedUser = mentionedUserId != null ? userReader.findActiveUserById(mentionedUserId) : null;

        Comment reply = Comment.reply(post, author, actualParent, mentionedUser, content);
        Comment savedReply = commentRepository.save(reply);
        communityNotificationService.sendReplyNotification(post, author, actualParent, mentionedUser, savedReply);
        return savedReply;
    }

    public Page<CommentResponse> getCommentsByPost(Long postId, Long viewerId, Pageable pageable) {
        Post post = findPostOrThrow(postId);
        Set<Long> blockedIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlocker_Id(viewerId));
        return commentRepository.findVisibleParentsByPost(post, pageable)
                .map(c -> CommentResponse.from(c, blockedIds));
    }

    public List<CommentResponse> getReplies(Long parentId, Long viewerId) {
        // 부모가 삭제됐더라도 살아있는 대댓글이 있으면 댓글 목록 응답에 placeholder 로 노출된다.
        // 클라가 placeholder 의 id 로 이 엔드포인트를 호출했을 때 404 가 나면 일관성이 깨지므로
        // findActiveCommentOrThrow 대신 deleted 여부를 가리지 않는 findById 로 조회한다.
        // 대댓글 자체는 deleted=false 만 반환하므로 placeholder + 빈 자식 케이스도 자연스럽게 빈 리스트.
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
        Set<Long> blockedIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlocker_Id(viewerId));
        return commentRepository.findByParentAndDeletedFalseOrderByCreatedAtAsc(parent).stream()
                .map(c -> CommentResponse.from(c, blockedIds))
                .toList();
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

    private Comment findActiveCommentOrThrow(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
    }
}
