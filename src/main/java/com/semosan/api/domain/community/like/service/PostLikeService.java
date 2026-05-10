package com.semosan.api.domain.community.like.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    @Transactional
    public boolean toggle(Long postId, Long userId) {
        Post post = findPostOrThrow(postId);
        User user = findUserOrThrow(userId);

        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            return false;
        }
        postLikeRepository.save(PostLike.create(post, user));
        return true;
    }

    public long count(Long postId) {
        Post post = findPostOrThrow(postId);
        return postLikeRepository.countByPost(post);
    }

    public boolean hasLiked(Long postId, Long userId) {
        Post post = findPostOrThrow(postId);
        User user = findUserOrThrow(userId);
        return postLikeRepository.existsByPostAndUser(post, user);
    }

    private Post findPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }
}
