package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.enums.TransportationType;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "transportations")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transportation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransportationType type;

    @Column(name = "direction", nullable = false, length = 50)
    private String direction;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    public static Transportation create(Mountain mountain, TransportationType type,
                                        String direction, String name, String description) {
        return Transportation.builder()
                .mountain(mountain)
                .type(type)
                .direction(direction)
                .name(name)
                .description(description)
                .build();
    }

    public void update(TransportationType type, String direction, String name, String description) {
        this.type = type;
        this.direction = direction;
        this.name = name;
        this.description = description;
    }
}
