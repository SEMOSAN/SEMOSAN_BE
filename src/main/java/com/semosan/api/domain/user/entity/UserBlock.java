package com.semosan.api.domain.user.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "user_blocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_blocks_blocker_blocked",
                        columnNames = {"blocker_id", "blocked_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_blocks_blocker_id", columnList = "blocker_id"),
                @Index(name = "idx_user_blocks_blocked_user_id", columnList = "blocked_user_id")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    public static UserBlock create(User blocker, User blockedUser) {
        return UserBlock.builder()
                .blocker(blocker)
                .blockedUser(blockedUser)
                .build();
    }
}
