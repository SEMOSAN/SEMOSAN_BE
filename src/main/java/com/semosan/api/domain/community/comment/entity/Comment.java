package com.semosan.api.domain.community.comment.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "comments")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_user_id")
    private User mentionedUser;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public static Comment create(Post post, User author, String content) {
        return Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();
    }

    public static Comment reply(
            Post post,
            User author,
            Comment parent,
            User mentionedUser,
            String content
    ) {
        return Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .mentionedUser(mentionedUser)
                .content(content)
                .build();
    }

    public void softDelete() {
        this.deleted = true;
    }

    public boolean isReply() {
        return this.parent != null;
    }
}
