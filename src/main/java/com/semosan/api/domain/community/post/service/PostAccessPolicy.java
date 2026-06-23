package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostAccessPolicy {

    private final UserBlockRepository userBlockRepository;

    // 게시글 상세 조회 시 차단한 작성자의 콘텐츠 접근을 막는 공통 정책.
    public void validateReadable(Long viewerId, Post post) {
        if (userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(viewerId, post.getAuthor().getId())) {
            throw new GeneralException(ErrorStatus.POST_AUTHOR_BLOCKED);
        }
    }
}
