package com.semosan.api.domain.community.post.service;

import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.repository.RecordPostRepository;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
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
    private final HikingMemberRepository hikingMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public RecordPost create(Long authorId, Long hikingRecordId, String content) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        HikingRecord hikingRecord = hikingRecordRepository.findById(hikingRecordId)
                .orElseThrow(() -> new IllegalArgumentException("등산 기록을 찾을 수 없습니다."));

        if (!hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, author)) {
            throw new IllegalStateException("본인이 참여한 등산 기록만 공유할 수 있습니다.");
        }

        RecordPost post = RecordPost.create(author, content, hikingRecord);
        return recordPostRepository.save(post);
    }

    public Page<RecordPost> getList(Pageable pageable) {
        return recordPostRepository.findAllByDeletedFalse(pageable);
    }

    public Page<RecordPost> getMyList(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
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
            throw new IllegalStateException("본인의 게시글만 삭제할 수 있습니다.");
        }
        post.softDelete();
    }

    private RecordPost findActivePostOrThrow(Long postId) {
        RecordPost post = recordPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        if (post.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글입니다.");
        }
        return post;
    }
}
