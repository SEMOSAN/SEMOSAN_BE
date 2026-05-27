package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseDifficultyFeedbackRepository extends JpaRepository<CourseDifficultyFeedback, Long> {

    boolean existsByHikingRecord_Id(Long hikingRecordId);
}
