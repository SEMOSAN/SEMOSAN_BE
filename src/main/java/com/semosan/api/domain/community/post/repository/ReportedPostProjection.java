package com.semosan.api.domain.community.post.repository;

import java.time.LocalDateTime;

public interface ReportedPostProjection {
    Long getPostId();
    String getTitle();
    String getContent();
    Long getAuthorId();
    String getAuthorNickname();
    long getReportCount();
    boolean getDeleted();
    LocalDateTime getCreatedAt();
}
