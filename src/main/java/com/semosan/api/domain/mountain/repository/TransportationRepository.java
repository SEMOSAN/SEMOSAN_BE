package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Transportation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportationRepository extends JpaRepository<Transportation, Long> {

    List<Transportation> findByMountainId(Long mountainId);
}
