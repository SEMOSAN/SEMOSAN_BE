package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import com.semosan.api.domain.hiking.enums.DifficultyFeedbackType;
import com.semosan.api.domain.mountain.enums.Difficulty;

public record CourseDifficultyFeedbackResponse(
        Long feedbackId,
        Long hikingRecordId,
        Long mountainId,
        String mountainName,
        Long courseId,
        String courseName,
        Difficulty guideDifficulty,
        DifficultyFeedbackType comparison
) {

    public static CourseDifficultyFeedbackResponse from(CourseDifficultyFeedback feedback) {
        return new CourseDifficultyFeedbackResponse(
                feedback.getId(),
                feedback.getHikingRecord().getId(),
                feedback.getCourse().getMountain().getId(),
                feedback.getCourse().getMountain().getName(),
                feedback.getCourse().getId(),
                feedback.getCourse().getName(),
                feedback.getGuideDifficulty(),
                feedback.getComparison()
        );
    }
}
