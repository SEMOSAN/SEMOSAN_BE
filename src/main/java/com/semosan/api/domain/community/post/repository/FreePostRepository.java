package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FreePostRepository extends JpaRepository<FreePost, Long> {

    Page<FreePost> findAllByDeletedFalse(Pageable pageable);

    Page<FreePost> findByAuthorAndDeletedFalse(User author, Pageable pageable);

    java.util.Optional<FreePost> findByIdAndDeletedFalse(Long id);

    @Query("""
            SELECT fp
            FROM FreePost fp
            WHERE fp.deleted = false
              AND fp.author.id NOT IN :blockedAuthorIds
            """)
    Page<FreePost> findVisibleExcludingAuthors(
            @Param("blockedAuthorIds") java.util.List<Long> blockedAuthorIds,
            Pageable pageable
    );

    @Query("SELECT fp FROM FreePost fp " +
            "WHERE fp.deleted = false " +
            "AND (LOWER(fp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(fp.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY fp.createdAt DESC")
    Page<FreePost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT fp FROM FreePost fp " +
            "WHERE fp.deleted = false " +
            "AND (LOWER(fp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(fp.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND fp.author.id NOT IN :blockedAuthorIds " +
            "ORDER BY fp.createdAt DESC")
    Page<FreePost> searchByKeywordExcludingAuthors(
            @Param("keyword") String keyword,
            @Param("blockedAuthorIds") java.util.List<Long> blockedAuthorIds,
            Pageable pageable
    );
}
