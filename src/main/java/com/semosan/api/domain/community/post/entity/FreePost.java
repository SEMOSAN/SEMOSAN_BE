package com.semosan.api.domain.community.post.entity;

import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "free_posts")
@Getter
@Entity
@DiscriminatorValue("FREE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreePost extends Post {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    private FreePost(User author, String title, String content) {
        super(author, content);
        this.title = title;
    }

    public static FreePost create(User author, String title, String content) {
        return new FreePost(author, title, content);
    }
}
