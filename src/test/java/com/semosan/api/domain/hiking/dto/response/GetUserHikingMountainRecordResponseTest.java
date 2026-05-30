package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.repository.projection.UserHikingMountainRecordProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GetUserHikingMountainRecordResponseTest {

    @Test
    void fromMapsImageUrlsInOrder() {
        GetUserHikingMountainRecordResponse response = GetUserHikingMountainRecordResponse.from(
                projection("near-summit-1", "near-summit-2")
        );

        assertThat(response.imageUrls()).containsExactly("near-summit-1", "near-summit-2");
        assertThat(response.lastHikedAt()).isEqualTo(LocalDate.of(2026, 5, 28));
    }

    @Test
    void fromFiltersBlankImageUrls() {
        GetUserHikingMountainRecordResponse response = GetUserHikingMountainRecordResponse.from(
                projection("", "near-summit-2")
        );

        assertThat(response.imageUrls()).containsExactly("near-summit-2");
    }

    private UserHikingMountainRecordProjection projection(String imageUrl1, String imageUrl2) {
        return new UserHikingMountainRecordProjection() {
            @Override
            public Long getMountainId() {
                return 1L;
            }

            @Override
            public String getMountainName() {
                return "관악산";
            }

            @Override
            public String getImageUrl1() {
                return imageUrl1;
            }

            @Override
            public String getImageUrl2() {
                return imageUrl2;
            }

            @Override
            public Long getHikingCount() {
                return 3L;
            }

            @Override
            public LocalDateTime getLastHikedAt() {
                return LocalDateTime.of(2026, 5, 28, 10, 0);
            }
        };
    }
}
