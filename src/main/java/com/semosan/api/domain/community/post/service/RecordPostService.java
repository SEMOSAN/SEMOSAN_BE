package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.repository.RecordPostRepository;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public RecordPost create(Long authorId, Long hikingRecordId, String content) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        HikingRecord hikingRecord = hikingRecordRepository.findById(hikingRecordId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.HIKING_RECORD_NOT_FOUND));

        RecordPost post = RecordPost.create(author, content, hikingRecord);
        return recordPostRepository.save(post);
    }

    public Page<RecordPost> getList(Pageable pageable) {
        return recordPostRepository.findAllByDeletedFalse(pageable);
    }

    public Page<RecordPost> getMyList(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        return recordPostRepository.findByAuthorAndDeletedFalse(author, pageable);
    }

    @Transactional
    public RecordPost getDetail(Long postId) {
        RecordPost post = findActivePostOrThrow(postId);
        post.increaseViewCount();
        return post;
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        RecordPost post = findActivePostOrThrow(postId);
        if (!post.getAuthor().getId().equals(requesterId)) {
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
