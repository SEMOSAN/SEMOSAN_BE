package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreePostRepository extends JpaRepository<FreePost, Long> {

    Page<FreePost> findAllByDeletedFalse(Pageable pageable);

    Page<FreePost> findByAuthorAndDeletedFalse(User author, Pageable pageable);
}
