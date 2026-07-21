package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "restaurant_sections")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "menu", length = 100)
    private String menu;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "map_url", columnDefinition = "TEXT")
    private String mapUrl;

    @Column(name = "blog_url", columnDefinition = "TEXT")
    private String blogUrl;

    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Restaurant> restaurants = new ArrayList<>();
}
