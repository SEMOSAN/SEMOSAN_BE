package com.semosan.api.domain.community.post.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.community.post.enums.FreePostReportReason;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "free_post_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_free_post_reports_reporter_post",
                        columnNames = {"reporter_id", "post_id"}
                )
        },
        indexes = {
                @Index(name = "idx_free_post_reports_post_id", columnList = "post_id"),
                @Index(name = "idx_free_post_reports_reporter_id", columnList = "reporter_id")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreePostReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private FreePost post;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private FreePostReportReason reason;

    public static FreePostReport create(User reporter, FreePost post, FreePostReportReason reason) {
        return FreePostReport.builder()
                .reporter(reporter)
                .post(post)
                .reason(reason)
                .build();
    }
}
