package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.MountainLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MountainLikeRepository extends JpaRepository<MountainLike, Long> {

    boolean existsByUser_IdAndMountain_Id(Long userId, Long mountainId);

    Optional<MountainLike> findByUser_IdAndMountain_Id(Long userId, Long mountainId);
}
