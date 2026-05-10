package com.semosan.api.domain.community.post.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "post_images")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    public static PostImage create(Post post, String imageUrl, int sortOrder, boolean main) {
        return PostImage.builder()
                .post(post)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .main(main)
                .build();
    }
}
