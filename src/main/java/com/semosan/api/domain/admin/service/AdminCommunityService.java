package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminUserSuspendRequest;
import com.semosan.api.domain.admin.dto.response.AdminReportedPostResponse;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.FreePostReportRepository;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.community.post.repository.ReportedPostProjection;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FreePostReportRepository freePostReportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminReportedPostResponse> getReportedPosts(Pageable pageable) {
        return freePostReportRepository.findReportedPosts(pageable)
                .map(p -> new AdminReportedPostResponse(
                        p.getPostId(),
                        p.getTitle(),
                        p.getContent(),
                        p.getAuthorId(),
                        p.getAuthorNickname(),
                        p.getReportCount(),
                        p.getDeleted(),
                        p.getCreatedAt()
                ));
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        post.softDelete();
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
        comment.softDelete();
    }

    @Transactional
    public void suspendUser(Long userId, AdminUserSuspendRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        user.suspend(request.suspendedUntil());
    }

    @Transactional
    public void unsuspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        user.unsuspend();
    }
}
