package com.semosan.api.domain.semofeed.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "semo_feeds",
        indexes = {
                @Index(name = "idx_semo_feeds_user", columnList = "user_id"),
                @Index(name = "idx_semo_feeds_public", columnList = "is_public")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SemoFeed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    public static SemoFeed create(User user, String imageUrl) {
        return SemoFeed.builder()
                .user(user)
                .imageUrl(imageUrl)
                .isPublic(false)
                .build();
    }

    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
