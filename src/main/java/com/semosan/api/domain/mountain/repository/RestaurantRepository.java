package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
