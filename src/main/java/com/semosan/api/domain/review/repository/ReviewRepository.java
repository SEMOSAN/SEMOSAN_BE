package com.semosan.api.domain.review.repository;

import com.semosan.api.domain.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "course"})
    @Query("SELECT r FROM Review r WHERE r.mountain.id = :mountainId ORDER BY r.createdAt DESC LIMIT :limit")
    List<Review> findRecentByMountainId(@Param("mountainId") Long mountainId, @Param("limit") int limit);
}
