package com.semosan.api.domain.community.post.dto;

import jakarta.validation.constraints.NotNull;

public record RecordPostCreateRequest(
        @NotNull Long hikingRecordId,
        String content
) {
}
