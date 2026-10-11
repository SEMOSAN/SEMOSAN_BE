package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.MountainLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MountainLikeRepository extends JpaRepository<MountainLike, Long> {

    boolean existsByUser_IdAndMountain_Id(Long userId, Long mountainId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MountainLike ml WHERE ml.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    /** delete(entity) 는 영향 행이 0이면 StaleStateException 이라, 동시 삭제를 흡수하려면 벌크 delete 여야 한다. */
    @Modifying
    @Query("DELETE FROM MountainLike ml WHERE ml.user.id = :userId AND ml.mountain.id = :mountainId")
    int deleteByUser_IdAndMountain_Id(@Param("userId") Long userId, @Param("mountainId") Long mountainId);

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

    /** 산마다 exists 를 돌리면 N+1 이라 페이지의 산 ID 를 한 번에 넘긴다. */
    @Query("SELECT ml.mountain.id FROM MountainLike ml "
            + "WHERE ml.user.id = :userId AND ml.mountain.id IN :mountainIds")
    List<Long> findLikedMountainIds(
            @Param("userId") Long userId,
            @Param("mountainIds") Collection<Long> mountainIds
    );

    /** 정렬이 없으면 페이지 간 중복·누락이 생긴다. ORDER BY 가 박혀 있어 Pageable.sort 는 무시됨 (서버 정책 우선). */
    @EntityGraph(attributePaths = "mountain")
    @Query(
            value = "SELECT ml FROM MountainLike ml WHERE ml.user.id = :userId "
                    + "ORDER BY ml.createdAt DESC, ml.id DESC",
            countQuery = "SELECT COUNT(ml) FROM MountainLike ml WHERE ml.user.id = :userId"
    )
    Page<MountainLike> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
