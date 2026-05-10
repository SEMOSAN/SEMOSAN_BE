package com.semosan.api.domain.community.comment.repository;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostAndParentIsNullAndDeletedFalse(Post post, Pageable pageable);

    List<Comment> findByParentAndDeletedFalseOrderByCreatedAtAsc(Comment parent);
}
