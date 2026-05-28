package com.semosan.api.domain.semofeed.repository;

import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.entity.SemoFeedEmoji;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SemoFeedEmojiRepository extends JpaRepository<SemoFeedEmoji, Long> {

    Optional<SemoFeedEmoji> findBySemoFeedAndUserAndEmojiType(
            SemoFeed semoFeed,
            User user,
            SemoFeedEmojiType emojiType
    );

    long countBySemoFeedAndEmojiType(SemoFeed semoFeed, SemoFeedEmojiType emojiType);

    @Query("""
            SELECT e.semoFeed.id, e.emojiType, COUNT(e)
            FROM SemoFeedEmoji e
            WHERE e.semoFeed.id IN :semoFeedIds
            GROUP BY e.semoFeed.id, e.emojiType
            """)
    List<Object[]> countBySemoFeedIdsGrouped(@Param("semoFeedIds") List<Long> semoFeedIds);

    @Query("""
            SELECT e.semoFeed.id, e.emojiType
            FROM SemoFeedEmoji e
            WHERE e.semoFeed.id IN :semoFeedIds
              AND e.user.id = :userId
            """)
    List<Object[]> findReactedTypesBySemoFeedIdsAndUserId(
            @Param("semoFeedIds") List<Long> semoFeedIds,
            @Param("userId") Long userId
    );
}
