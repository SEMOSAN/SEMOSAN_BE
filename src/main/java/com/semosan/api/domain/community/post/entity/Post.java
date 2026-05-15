package com.semosan.api.domain.community.post.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "posts")
@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "post_type", length = 20)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    protected Post(User author, String content) {
        this.author = author;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
