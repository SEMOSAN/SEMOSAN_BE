package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.CourseDifficultyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseDifficultyFeedbackRepository extends JpaRepository<CourseDifficultyFeedback, Long> {

    boolean existsByHikingRecord_Id(Long hikingRecordId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CourseDifficultyFeedback cdf WHERE cdf.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CourseDifficultyFeedback cdf WHERE cdf.hikingRecord.id IN :hikingRecordIds")
    void deleteByHikingRecordIdIn(@Param("hikingRecordIds") List<Long> hikingRecordIds);
}
