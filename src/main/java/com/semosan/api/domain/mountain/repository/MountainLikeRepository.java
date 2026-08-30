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

    /**
     * 유니크 제약(user_id, mountain_id) 충돌 시 예외 없이 무시한다. 동시 좋아요 요청에서
     * DataIntegrityViolationException을 유발하지 않기 위한 upsert.
     * @return 실제로 insert된 row 수 (0 = 이미 좋아요 존재)
     */
    @Modifying
    @Query(value = "INSERT INTO mountain_likes (user_id, mountain_id, created_at, updated_at) "
            + "VALUES (:userId, :mountainId, now(), now()) "
            + "ON CONFLICT (user_id, mountain_id) DO NOTHING", nativeQuery = true)
    int insertIgnoreConflict(@Param("userId") Long userId, @Param("mountainId") Long mountainId);

    @EntityGraph(attributePaths = "mountain")
    @Query(
            value = "SELECT ml FROM MountainLike ml WHERE ml.user.id = :userId",
            countQuery = "SELECT COUNT(ml) FROM MountainLike ml WHERE ml.user.id = :userId"
    )
    Page<MountainLike> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
