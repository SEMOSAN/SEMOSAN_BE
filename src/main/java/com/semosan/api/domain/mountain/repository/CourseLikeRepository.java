package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.CourseLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseLikeRepository extends JpaRepository<CourseLike, Long> {

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    Optional<CourseLike> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CourseLike cl WHERE cl.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    /**
     * 유니크 제약(user_id, course_id) 충돌 시 예외 없이 무시한다. 동시 좋아요 요청에서
     * DataIntegrityViolationException을 유발하지 않기 위한 upsert.
     * @return 실제로 insert된 row 수 (0 = 이미 좋아요 존재)
     */
    @Modifying
    @Query(value = "INSERT INTO course_likes (user_id, course_id, created_at, updated_at) "
            + "VALUES (:userId, :courseId, now(), now()) "
            + "ON CONFLICT (user_id, course_id) DO NOTHING", nativeQuery = true)
    int insertIgnoreConflict(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
