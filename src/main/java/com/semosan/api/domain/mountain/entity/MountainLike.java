package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "mountain_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mountain_likes_user_id_mountain_id",
                        columnNames = {"user_id", "mountain_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mountain_likes_user_id_created_at", columnList = "user_id, created_at DESC")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MountainLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    // 사용자와 산 정보로 MountainLike 엔티티를 생성합니다.
    public static MountainLike create(User user, Mountain mountain) {
        return MountainLike.builder()
                .user(user)
                .mountain(mountain)
                .build();
    }
}
