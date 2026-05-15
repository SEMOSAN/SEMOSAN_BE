package com.semosan.api.domain.community.like.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_like_post_user",
                        columnNames = {"post_id", "user_id"}
                )
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static PostLike create(Post post, User user) {
        return PostLike.builder()
                .post(post)
                .user(user)
                .build();
    }
}
