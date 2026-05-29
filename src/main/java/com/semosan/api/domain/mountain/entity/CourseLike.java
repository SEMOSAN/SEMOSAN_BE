package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "course_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_likes_user_id_course_id",
                        columnNames = {"user_id", "course_id"}
                )
        },
        indexes = {
                @Index(name = "idx_course_likes_user_id_created_at", columnList = "user_id, created_at DESC")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public static CourseLike create(User user, Course course) {
        return CourseLike.builder()
                .user(user)
                .course(course)
                .build();
    }
}
