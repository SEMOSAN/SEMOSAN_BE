package com.semosan.api.domain.community.post.entity;

import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "record_posts")
@Getter
@Entity
@DiscriminatorValue("RECORD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordPost extends Post {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiking_record_id", nullable = false)
    private HikingRecord hikingRecord;

    private RecordPost(User author, String content, HikingRecord hikingRecord) {
        super(author, content);
        this.hikingRecord = hikingRecord;
    }

    public static RecordPost create(User author, String content, HikingRecord hikingRecord) {
        return new RecordPost(author, content, hikingRecord);
    }
}
