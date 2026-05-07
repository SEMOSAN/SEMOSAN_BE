package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Mountain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MountainRepository extends JpaRepository<Mountain, Long> {

    @Query("SELECT m FROM Mountain m WHERE m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%')")
    Page<Mountain> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
