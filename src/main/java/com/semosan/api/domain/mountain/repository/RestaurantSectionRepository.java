package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.RestaurantSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantSectionRepository extends JpaRepository<RestaurantSection, Long> {

    @Query("SELECT DISTINCT s FROM RestaurantSection s LEFT JOIN FETCH s.restaurants WHERE s.mountain.id = :mountainId")
    List<RestaurantSection> findByMountainIdWithRestaurants(@Param("mountainId") Long mountainId);
}
