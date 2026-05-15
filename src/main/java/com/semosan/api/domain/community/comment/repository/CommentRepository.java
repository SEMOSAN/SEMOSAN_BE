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

    Page<Comment> findByPostAndParentIsNullAndDeletedFalse(Post post, Pageable pageable);

    List<Comment> findByParentAndDeletedFalseOrderByCreatedAtAsc(Comment parent);

    long countByPostAndDeletedFalse(Post post);

    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.deleted = false GROUP BY c.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
