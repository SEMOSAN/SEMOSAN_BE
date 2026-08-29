package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainLikeToggleResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.service.MountainLikeService;
import com.semosan.api.domain.mountain.service.MountainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MountainControllerTest {

    @Mock
    private MountainService mountainService;

    @Mock
    private MountainLikeService mountainLikeService;

    @InjectMocks
    private MountainController mountainController;

    @Test
    void getMountainsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        MountainListResponse mountain = mountainListResponse();
        when(mountainService.getMountains(1L, pageable)).thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> response =
                mountainController.getMountains(1L, pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData().content()).containsExactly(mountain);
    }

    @Test
    void searchMountainsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainService.searchMountains(1L, "관악", pageable))
                .thenReturn(new PageImpl<>(List.of(mountainListResponse()), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> response =
                mountainController.searchMountains(1L, "관악", pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_SEARCH_SUCCESS.getHttpStatus());
        verify(mountainService).searchMountains(1L, "관악", pageable);
    }

    @Test
    void getMountainsForMapReturnsSuccessResponse() {
        MountainMapListResponse mapResponse = new MountainMapListResponse(true, List.of());
        when(mountainService.getMountainsForMap(1L, null, null, null, null)).thenReturn(mapResponse);

        ResponseEntity<ApiResponse<MountainMapListResponse>> response =
                mountainController.getMountainsForMap(1L, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_MAP_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(mapResponse);
    }

    @Test
    void getRecommendedMountainsReturnsSuccessResponse() {
        MountainRecommendationResponse recommendation =
                new MountainRecommendationResponse(1L, "관악산", "image.jpg", "중", 632, "서울");
        when(mountainService.getRecommendedMountains(1L, 37.5, 127.0)).thenReturn(List.of(recommendation));

        ResponseEntity<ApiResponse<List<MountainRecommendationResponse>>> response =
                mountainController.getRecommendedMountains(1L, 37.5, 127.0);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_RECOMMENDATION_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).containsExactly(recommendation);
    }

    @Test
    void getLikedMountainsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        LikedMountainResponse liked = new LikedMountainResponse(
                1L, "관악산", "서울", 632.2, Difficulty.NORMAL, List.of("image.jpg"));
        when(mountainLikeService.getLikedMountains(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(liked), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<LikedMountainResponse>>> response =
                mountainController.getLikedMountains(1L, pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LIKED_MOUNTAIN_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData().content()).containsExactly(liked);
    }

    @Test
    void getMountainDetailReturnsSuccessResponse() {
        MountainDetailResponse detail = new MountainDetailResponse(null, List.of(), null, null, List.of(), List.of());
        when(mountainService.getMountainDetail(1L, 10L)).thenReturn(detail);

        ResponseEntity<ApiResponse<MountainDetailResponse>> response =
                mountainController.getMountainDetail(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_DETAIL_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(detail);
    }

    @Test
    void toggleMountainLikeDelegatesAndReturnsSuccessResponse() {
        MountainLikeToggleResponse toggleResponse = new MountainLikeToggleResponse(true);
        when(mountainLikeService.toggleMountainLike(1L, 10L)).thenReturn(toggleResponse);

        ResponseEntity<ApiResponse<MountainLikeToggleResponse>> response = mountainController.toggleMountainLike(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.MOUNTAIN_LIKE_TOGGLE_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(toggleResponse);
    }

    private MountainListResponse mountainListResponse() {
        return new MountainListResponse(
                1L, "관악산", "서울", 632.2, Difficulty.NORMAL, 120,
                List.of("image.jpg"), 37.5, 127.0
        );
    }
}
