package com.semosan.api.domain.tracking.dto.response;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NearbyMountainResponseTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serializesDropdownIdsAndNamesAlongsideExistingFields() {
        NearbyMountainResponse response = new NearbyMountainResponse(
                new NearbyMountainResponse.NearbyMountainInfo(
                        1L, "관악산", "서울", 632.2, 37.5, 127.0, List.of()),
                List.of(new NearbyMountainResponse.CourseInfo(10L, "정상 코스", null, 1500.0, 90)),
                List.of(new NearbyMountainResponse.NearbyMountainOption(1L, "관악산"),
                        new NearbyMountainResponse.NearbyMountainOption(2L, "삼성산"))
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.path("mountain").path("mountainId").asLong()).isEqualTo(1L);
        assertThat(json.path("courses").get(0).path("courseId").asLong()).isEqualTo(10L);
        assertThat(json.path("nearbyMountains").size()).isEqualTo(2);
        assertThat(json.path("nearbyMountains").get(0).size()).isEqualTo(2);
        assertThat(json.path("nearbyMountains").get(0).path("mountainId").asLong()).isEqualTo(1L);
        assertThat(json.path("nearbyMountains").get(1).path("name").asText()).isEqualTo("삼성산");
    }

    @Test
    void serializesNoNearbyMountainsAsEmptyArray() {
        NearbyMountainResponse response = new NearbyMountainResponse(null, List.of(), List.of());

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.path("nearbyMountains").isArray()).isTrue();
        assertThat(json.path("nearbyMountains").size()).isZero();
    }
}
