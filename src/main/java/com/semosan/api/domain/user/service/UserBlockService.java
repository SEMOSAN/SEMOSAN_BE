package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserBlock;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
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
            if (!isUniqueViolation(e)) {
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

    private boolean isUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && BLOCK_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(BLOCK_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private User findActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
