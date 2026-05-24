package com.semosan.api.domain.semofeed.repository;

import com.semosan.api.domain.semofeed.entity.SemoFeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SemoFeedRepository extends JpaRepository<SemoFeed, Long> {

    @Query("SELECT s FROM SemoFeed s WHERE s.isPublic = true ORDER BY s.createdAt DESC")
    Page<SemoFeed> findPublic(Pageable pageable);

    @Query("SELECT s FROM SemoFeed s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<SemoFeed> findByUserId(Long userId);
}
