package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.hiking.dto.HikingRecordSummaryResponse;

import java.time.LocalDateTime;

public record RecordPostResponse(
        Long id,
        AuthorResponse author,
        String content,
        HikingRecordSummaryResponse hikingRecord,
        Integer viewCount,
        LocalDateTime createdAt
) {
    public static RecordPostResponse from(RecordPost post) {
        return from(post, post.getViewCount());
    }

    public static RecordPostResponse from(RecordPost post, int viewCount) {
        return new RecordPostResponse(
                post.getId(),
                AuthorResponse.from(post.getAuthor()),
                post.getContent(),
                HikingRecordSummaryResponse.from(post.getHikingRecord()),
                viewCount,
                post.getCreatedAt()
        );
    }
}
