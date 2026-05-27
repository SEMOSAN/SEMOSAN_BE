package com.semosan.api.domain.hiking.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.hiking.enums.DifficultyFeedbackType;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(
        name = "course_difficulty_feedbacks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_difficulty_feedback_hiking_record",
                        columnNames = "hiking_record_id"
                )
        },
        indexes = {
                @Index(name = "idx_course_difficulty_feedback_course_id", columnList = "course_id"),
                @Index(name = "idx_course_difficulty_feedback_user_id", columnList = "user_id")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseDifficultyFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiking_record_id", nullable = false)
    private HikingRecord hikingRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "guide_difficulty", nullable = false, length = 20)
    private Difficulty guideDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison", nullable = false, length = 20)
    private DifficultyFeedbackType comparison;

    public static CourseDifficultyFeedback create(
            HikingRecord hikingRecord,
            User user,
            DifficultyFeedbackType comparison
    ) {
        Course course = hikingRecord.getCourse();
        if (course == null) {
            throw new IllegalArgumentException("course is required");
        }
        return CourseDifficultyFeedback.builder()
                .hikingRecord(hikingRecord)
                .user(user)
                .course(course)
                .guideDifficulty(course.getDifficulty())
                .comparison(comparison)
                .build();
    }
}
