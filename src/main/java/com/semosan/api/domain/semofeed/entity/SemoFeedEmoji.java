package com.semosan.api.domain.semofeed.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "semo_feed_emojis",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_semo_feed_emoji_feed_user_type",
                        columnNames = {"semo_feed_id", "user_id", "emoji_type"}
                )
        },
        indexes = {
                @Index(name = "idx_semo_feed_emojis_feed", columnList = "semo_feed_id"),
                @Index(name = "idx_semo_feed_emojis_user", columnList = "user_id")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SemoFeedEmoji extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semo_feed_id", nullable = false)
    private SemoFeed semoFeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "emoji_type", nullable = false, length = 20)
    private SemoFeedEmojiType emojiType;

    public static SemoFeedEmoji create(SemoFeed semoFeed, User user, SemoFeedEmojiType emojiType) {
        return SemoFeedEmoji.builder()
                .semoFeed(semoFeed)
                .user(user)
                .emojiType(emojiType)
                .build();
    }
}
