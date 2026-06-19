package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.repository.RecordPostRepository;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordPostService {

    private final RecordPostRepository recordPostRepository;
    private final HikingRecordRepository hikingRecordRepository;
    private final HikingMemberRepository hikingMemberRepository;
    private final PostBlockPolicy postBlockPolicy;
    private final UserReader userReader;

    @Transactional
    public RecordPost create(Long authorId, Long hikingRecordId, String content) {
        User author = userReader.findActiveUserById(authorId);
        HikingRecord hikingRecord = hikingRecordRepository.findById(hikingRecordId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.HIKING_RECORD_NOT_FOUND));

        if (!hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, author)) {
            throw new GeneralException(ErrorStatus.HIKING_RECORD_FORBIDDEN);
        }

        RecordPost post = RecordPost.create(author, content, hikingRecord);
        return recordPostRepository.save(post);
    }

    public Page<RecordPost> getList(Long viewerId, Pageable pageable) {
        return recordPostRepository.findVisibleByViewerId(viewerId, pageable);
    }

    public Page<RecordPost> getMyList(Long authorId, Pageable pageable) {
        User author = userReader.findActiveUserById(authorId);
        return recordPostRepository.findByAuthorAndDeletedFalse(author, pageable);
    }

    @Transactional
    public RecordPost getDetail(Long viewerId, Long postId) {
        RecordPost post = findActivePostOrThrow(postId);
        postBlockPolicy.validateReadable(viewerId, post);
        post.increaseViewCount();
        return post;
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        RecordPost post = findActivePostOrThrow(postId);
        if (!post.isOwnedBy(requesterId)) {
            throw new GeneralException(ErrorStatus.POST_FORBIDDEN);
        }
        post.softDelete();
    }

    private RecordPost findActivePostOrThrow(Long postId) {
        RecordPost post = recordPostRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorStatus.POST_DELETED);
        }
        return post;
    }
}
