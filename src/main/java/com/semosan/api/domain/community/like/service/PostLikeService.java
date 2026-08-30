package com.semosan.api.domain.community.like.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.event.PostLikedEvent;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserReader userReader;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @return true = 좋아요 누름 / false = 좋아요 취소
     */
    private boolean toggle(Long postId, Long userId) {
        Post post = findPostOrThrow(postId);
        User user = findUserOrThrow(userId);

        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            return false;
        }

        // ON CONFLICT DO NOTHING이라 동시 요청이 겹쳐도 예외 없이 0 row로 끝난다.
        if (postLikeRepository.insertIgnoreConflict(postId, userId) > 0) {
            eventPublisher.publishEvent(new PostLikedEvent(post.getId(), user.getId()));
        }
        return true;
    }

    public long count(Long postId) {
        Post post = findPostOrThrow(postId);
        return postLikeRepository.countByPost(post);
    }

    @Transactional
    public PostLikeToggleResponse toggleWithCount(Long postId, Long userId) {
        boolean liked = this.toggle(postId, userId);
        long count = this.count(postId);
        return new PostLikeToggleResponse(liked, count);
    }

    public boolean hasLiked(Long postId, Long userId) {
        Post post = findPostOrThrow(postId);
        User user = findUserOrThrow(userId);
        return postLikeRepository.existsByPostAndUser(post, user);
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userReader.findActiveUserById(userId);
    }
}
