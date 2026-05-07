package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByMountainId(Long mountainId);
}
