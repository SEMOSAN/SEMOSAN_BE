package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordPostRepository extends JpaRepository<RecordPost, Long> {

    Page<RecordPost> findAllByDeletedFalse(Pageable pageable);

    Page<RecordPost> findByAuthorAndDeletedFalse(User author, Pageable pageable);
}
