package com.semosan.api.domain.hiking.dto.request;

import com.semosan.api.domain.hiking.enums.DifficultyFeedbackType;
import jakarta.validation.constraints.NotNull;

public record CreateCourseDifficultyFeedbackRequest(
        @NotNull DifficultyFeedbackType comparison
) {
}
