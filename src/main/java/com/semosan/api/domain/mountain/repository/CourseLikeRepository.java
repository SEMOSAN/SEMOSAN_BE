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

    long countByCourse_Id(Long courseId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CourseLike cl WHERE cl.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
