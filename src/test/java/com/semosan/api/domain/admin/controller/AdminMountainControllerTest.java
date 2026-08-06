package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.admin.dto.request.AdminTransportationRequest;
import com.semosan.api.domain.admin.dto.response.AdminMountainListResponse;
import com.semosan.api.domain.admin.service.AdminMountainService;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
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
class AdminMountainControllerTest {

    @Mock
    private AdminMountainService adminMountainService;

    @InjectMocks
    private AdminMountainController adminMountainController;

    @Test
    void getMountainsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminMountainListResponse mountain = new AdminMountainListResponse(
                1L, "관악산", "서울", 632.2, Difficulty.NORMAL, 120,
                List.of("image.jpg"), 37.5, 127.0, true, 4L
        );
        when(adminMountainService.getMountains("관악", "PUBLIC", pageable))
                .thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<AdminMountainListResponse>>> response =
                adminMountainController.getMountains("관악", "PUBLIC", pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_MOUNTAIN_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData().content()).containsExactly(mountain);
    }

    @Test
    void getMountainDetailReturnsSuccessResponse() {
        MountainDetailResponse detail = new MountainDetailResponse(null, List.of(), null, null, List.of(), List.of());
        when(adminMountainService.getMountainDetail(1L)).thenReturn(detail);

        ResponseEntity<ApiResponse<MountainDetailResponse>> response = adminMountainController.getMountainDetail(1L);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.ADMIN_MOUNTAIN_DETAIL_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(detail);
    }

    @Test
    void mountainMutationEndpointsDelegateAndReturnSuccessResponses() {
        AdminMountainUpdateRequest updateRequest = new AdminMountainUpdateRequest(
                "북한산", "서울", 836.5, Difficulty.HARD, 240, List.of("image.jpg"));
        AdminMountainVisibilityRequest visibilityRequest = new AdminMountainVisibilityRequest(false);

        assertThat(adminMountainController.updateMountain(1L, updateRequest).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_MOUNTAIN_UPDATE_SUCCESS.getHttpStatus());
        assertThat(adminMountainController.updateVisibility(1L, visibilityRequest).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_MOUNTAIN_VISIBILITY_UPDATE_SUCCESS.getHttpStatus());
        verify(adminMountainService).updateMountain(1L, updateRequest);
        verify(adminMountainService).updateVisibility(1L, visibilityRequest);
    }

    @Test
    void restaurantSectionEndpointsDelegateAndReturnSuccessResponses() {
        AdminRestaurantSectionRequest request = new AdminRestaurantSectionRequest("맛집");
        when(adminMountainService.createRestaurantSection(1L, request)).thenReturn(10L);

        assertThat(adminMountainController.createRestaurantSection(1L, request).getBody().getData()).isEqualTo(10L);
        assertThat(adminMountainController.updateRestaurantSection(10L, request).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_RESTAURANT_SECTION_UPDATE_SUCCESS.getHttpStatus());
        assertThat(adminMountainController.deleteRestaurantSection(10L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_RESTAURANT_SECTION_DELETE_SUCCESS.getHttpStatus());
        verify(adminMountainService).createRestaurantSection(1L, request);
        verify(adminMountainService).updateRestaurantSection(10L, request);
        verify(adminMountainService).deleteRestaurantSection(10L);
    }

    @Test
    void restaurantEndpointsDelegateAndReturnSuccessResponses() {
        AdminRestaurantRequest request =
                new AdminRestaurantRequest("식당", "한식", "메뉴", "설명", "image", "map", "blog");
        when(adminMountainService.createRestaurant(10L, request)).thenReturn(20L);

        assertThat(adminMountainController.createRestaurant(10L, request).getBody().getData()).isEqualTo(20L);
        assertThat(adminMountainController.updateRestaurant(20L, request).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_RESTAURANT_UPDATE_SUCCESS.getHttpStatus());
        assertThat(adminMountainController.deleteRestaurant(20L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_RESTAURANT_DELETE_SUCCESS.getHttpStatus());
        verify(adminMountainService).createRestaurant(10L, request);
        verify(adminMountainService).updateRestaurant(20L, request);
        verify(adminMountainService).deleteRestaurant(20L);
    }

    @Test
    void transportationEndpointsDelegateAndReturnSuccessResponses() {
        AdminTransportationRequest request =
                new AdminTransportationRequest(TransportationType.BUS, "상행", "버스", "설명");
        when(adminMountainService.createTransportation(1L, request)).thenReturn(30L);

        assertThat(adminMountainController.createTransportation(1L, request).getBody().getData()).isEqualTo(30L);
        assertThat(adminMountainController.updateTransportation(30L, request).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_TRANSPORTATION_UPDATE_SUCCESS.getHttpStatus());
        assertThat(adminMountainController.deleteTransportation(30L).getStatusCode())
                .isEqualTo(SuccessStatus.ADMIN_TRANSPORTATION_DELETE_SUCCESS.getHttpStatus());
        verify(adminMountainService).createTransportation(1L, request);
        verify(adminMountainService).updateTransportation(30L, request);
        verify(adminMountainService).deleteTransportation(30L);
    }
}
