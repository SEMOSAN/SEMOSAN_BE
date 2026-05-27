package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.enums.FreePostReportReason;
import jakarta.validation.constraints.NotNull;

public record FreePostReportRequest(
        @NotNull FreePostReportReason reason
) {
}
