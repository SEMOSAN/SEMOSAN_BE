package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.MountainLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MountainLikeRepository extends JpaRepository<MountainLike, Long> {

    boolean existsByUser_IdAndMountain_Id(Long userId, Long mountainId);

    Optional<MountainLike> findByUser_IdAndMountain_Id(Long userId, Long mountainId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MountainLike ml WHERE ml.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "mountain")
    @Query(
            value = "SELECT ml FROM MountainLike ml WHERE ml.user.id = :userId",
            countQuery = "SELECT COUNT(ml) FROM MountainLike ml WHERE ml.user.id = :userId"
    )
    Page<MountainLike> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
