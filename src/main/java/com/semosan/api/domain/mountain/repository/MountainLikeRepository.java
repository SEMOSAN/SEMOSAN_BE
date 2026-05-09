package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.MountainLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MountainLikeRepository extends JpaRepository<MountainLike, Long> {

    boolean existsByUser_IdAndMountain_Id(Long userId, Long mountainId);
}
