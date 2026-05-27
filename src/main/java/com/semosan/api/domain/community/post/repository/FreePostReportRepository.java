package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.FreePostReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreePostReportRepository extends JpaRepository<FreePostReport, Long> {

    boolean existsByReporter_IdAndPost_Id(Long reporterId, Long postId);
}
