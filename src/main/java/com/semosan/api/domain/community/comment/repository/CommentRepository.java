package com.semosan.api.domain.community.comment.repository;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndDeletedFalse(Long id);

    /**
     * 게시글의 최상위 댓글 목록을 페이징 조회.
     * 삭제된 부모라도 살아있는 대댓글이 하나라도 있으면 placeholder("삭제된 댓글입니다") 노출용으로 함께 반환한다.
     * 자식이 전부 삭제됐거나 처음부터 없는 삭제 부모는 응답에서 제외해 노이즈를 줄인다.
     */
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parent IS NULL " +
            "AND (c.deleted = false " +
            "     OR EXISTS (SELECT 1 FROM Comment r WHERE r.parent = c AND r.deleted = false))")
    Page<Comment> findVisibleParentsByPost(@Param("post") Post post, Pageable pageable);

    List<Comment> findByParentAndDeletedFalseOrderByCreatedAtAsc(Comment parent);

    long countByPostAndDeletedFalse(Post post);

    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.deleted = false GROUP BY c.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
