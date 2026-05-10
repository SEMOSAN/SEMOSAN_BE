package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.PostImage;

import java.time.LocalDateTime;
import java.util.List;

public record FreePostDetailResponse(
        Long id,
        AuthorResponse author,
        String title,
        String content,
        List<PostImageResponse> images,
        Integer viewCount,
        Long likeCount,
        Long commentCount,
        LocalDateTime createdAt
) {
    public static FreePostDetailResponse from(
            FreePost post,
            List<PostImage> images,
            long likeCount,
            long commentCount
    ) {
        return new FreePostDetailResponse(
                post.getId(),
                AuthorResponse.from(post.getAuthor()),
                post.getTitle(),
                post.getContent(),
                images.stream().map(PostImageResponse::from).toList(),
                post.getViewCount(),
                likeCount,
                commentCount,
                post.getCreatedAt()
        );
    }
}
