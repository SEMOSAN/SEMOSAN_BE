package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.FreePostReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FreePostReportRepository extends JpaRepository<FreePostReport, Long> {

    boolean existsByReporter_IdAndPost_Id(Long reporterId, Long postId);

    @Query("""
            SELECT r.post.id AS postId, r.post.title AS title, r.post.content AS content,
                   r.post.author.id AS authorId, r.post.author.nickname AS authorNickname,
                   COUNT(r) AS reportCount, r.post.deleted AS deleted, r.post.createdAt AS createdAt
            FROM FreePostReport r
            GROUP BY r.post.id, r.post.title, r.post.content,
                     r.post.author.id, r.post.author.nickname,
                     r.post.deleted, r.post.createdAt
            ORDER BY COUNT(r) DESC, r.post.createdAt DESC
            """)
    Page<ReportedPostProjection> findReportedPosts(Pageable pageable);
}
