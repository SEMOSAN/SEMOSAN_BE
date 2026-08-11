package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GetUserHikingRecordResponseTest {

    @Test
    void fromMapsSessionId() {
        GetUserHikingRecordResponse response = GetUserHikingRecordResponse.from(projection());

        assertThat(response.hikingRecordId()).isEqualTo(1L);
        assertThat(response.sessionId()).isEqualTo(10L);
        assertThat(response.imageUrls()).containsExactly("photo-report", "clive");
        assertThat(response.hikedAt()).isEqualTo(LocalDate.of(2026, 5, 28));
    }

    @Test
    void fromIncludesOnlyPhotoReportImageWhenCliveImageIsNull() {
        GetUserHikingRecordResponse response = GetUserHikingRecordResponse.from(projection("photo-report", null));

        assertThat(response.imageUrls()).containsExactly("photo-report");
    }

    @Test
    void fromIncludesOnlyCliveImageWhenPhotoReportImageIsNull() {
        GetUserHikingRecordResponse response = GetUserHikingRecordResponse.from(projection(null, "clive"));

        assertThat(response.imageUrls()).containsExactly("clive");
    }

    @Test
    void fromReturnsEmptyImageUrlsWhenBothImagesAreNull() {
        GetUserHikingRecordResponse response = GetUserHikingRecordResponse.from(projection(null, null));

        assertThat(response.imageUrls()).isEmpty();
    }

    private UserHikingRecordProjection projection() {
        return projection("photo-report", "clive");
    }

    private UserHikingRecordProjection projection(String photoReportImageUrl, String cliveImageUrl) {
        return new UserHikingRecordProjection() {
            @Override
            public Long getHikingRecordId() {
                return 1L;
            }

            @Override
            public Long getSessionId() {
                return 10L;
            }

            @Override
            public String getRecordName() {
                return null;
            }

            @Override
            public Long getMountainId() {
                return 2L;
            }

            @Override
            public String getMountainName() {
                return "관악산";
            }

            @Override
            public Long getCourseId() {
                return 3L;
            }

            @Override
            public String getCourseName() {
                return "연주대 코스";
            }

            @Override
            public String getPhotoReportImageUrl() {
                return photoReportImageUrl;
            }

            @Override
            public String getCliveImageUrl() {
                return cliveImageUrl;
            }

            @Override
            public Double getDistance() {
                return 6200.0;
            }

            @Override
            public Integer getDuration() {
                return 3600;
            }

            @Override
            public LocalDateTime getHikedAt() {
                return LocalDateTime.of(2026, 5, 28, 10, 0);
            }
        };
    }
}
