package com.semosan.api.domain.community.like.event;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.domain.community.notification.service.CommunityNotificationService;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikedEventListener {

    private final PostRepository postRepository;
    private final UserReader userReader;
    private final CommunityNotificationService communityNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onPostLiked(PostLikedEvent event) {
        try {
            Post post = postRepository.findByIdAndDeletedFalse(event.postId())
                    .orElse(null);
            if (post == null) {
                return;
            }
            User actor = userReader.findActiveUserById(event.actorId());
            communityNotificationService.sendPostLikeNotification(post, actor);
        } catch (GeneralException e) {
            log.warn("게시글 좋아요 알림 생성 실패: postId={}, actorId={}, code={}, msg={}",
                    event.postId(), event.actorId(), e.getErrorStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("게시글 좋아요 알림 생성 중 알 수 없는 오류: postId={}, actorId={}, msg={}",
                    event.postId(), event.actorId(), e.getMessage(), e);
        }
    }
}
