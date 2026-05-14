package com.semosan.api.domain.community.like.service;

import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

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

        try {
            postLikeRepository.save(PostLike.create(post, user));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("PostLike 동시 요청 감지: postId={}, userId={}", postId, userId);
            return true;
        }
    }

    public long count(Long postId) {
        Post post = findPostOrThrow(postId);
        return postLikeRepository.countByPost(post);
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
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
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }
}
