package com.semosan.api.domain.hiking.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(
        name = "hiking_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hiking_member_record_user",
                        columnNames = {"hiking_record_id", "user_id"}
                )
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HikingMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiking_record_id", nullable = false)
    private HikingRecord hikingRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static HikingMember create(HikingRecord hikingRecord, User user) {
        return HikingMember.builder()
                .hikingRecord(hikingRecord)
                .user(user)
                .build();
    }
}
