package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.ConstraintViolationUtils;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserBlock;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private static final String BLOCK_UNIQUE_CONSTRAINT = "uk_user_blocks_blocker_blocked";

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final FreePostRepository freePostRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void block(Long blockerId, Long blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new GeneralException(ErrorStatus.USER_BLOCK_SELF_NOT_ALLOWED);
        }
        User blocker = findActiveUserOrThrow(blockerId);
        User blockedUser = findActiveUserOrThrow(blockedUserId);

        if (userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(blockerId, blockedUserId)) {
            return;
        }

        try {
            userBlockRepository.saveAndFlush(UserBlock.create(blocker, blockedUser));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유니크 제약 위반 시 멱등 처리 (이미 차단된 상태로 간주)
            if (!ConstraintViolationUtils.isViolation(e, BLOCK_UNIQUE_CONSTRAINT)) {
                throw e;
            }
        }
    }

    @Transactional
    public void blockByPost(Long blockerId, Long postId) {
        FreePost post = freePostRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        block(blockerId, post.getAuthor().getId());
    }

    @Transactional
    public void blockByComment(Long blockerId, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
        block(blockerId, comment.getAuthor().getId());
    }

    private User findActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
