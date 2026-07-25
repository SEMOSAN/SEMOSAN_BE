package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.controller.docs.AdminMountainControllerDocs;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.admin.service.AdminMountainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMountainController implements AdminMountainControllerDocs {

    private final AdminMountainService adminMountainService;

    @PutMapping("/mountains/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateMountain(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminMountainUpdateRequest request
    ) {
        adminMountainService.updateMountain(mountainId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_UPDATE_SUCCESS);
    }

    @PatchMapping("/mountains/{mountainId}/visibility")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminMountainVisibilityRequest request
    ) {
        adminMountainService.updateVisibility(mountainId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_VISIBILITY_UPDATE_SUCCESS);
    }

    @PostMapping("/mountains/{mountainId}/restaurant-sections")
    @Override
    public ResponseEntity<ApiResponse<Long>> createRestaurantSection(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminRestaurantSectionRequest request
    ) {
        Long sectionId = adminMountainService.createRestaurantSection(mountainId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_SECTION_CREATE_SUCCESS, sectionId);
    }

    @PutMapping("/restaurant-sections/{sectionId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateRestaurantSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody AdminRestaurantSectionRequest request
    ) {
        adminMountainService.updateRestaurantSection(sectionId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_SECTION_UPDATE_SUCCESS);
    }

    @DeleteMapping("/restaurant-sections/{sectionId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteRestaurantSection(@PathVariable Long sectionId) {
        adminMountainService.deleteRestaurantSection(sectionId);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_SECTION_DELETE_SUCCESS);
    }

    @PostMapping("/restaurant-sections/{sectionId}/restaurants")
    @Override
    public ResponseEntity<ApiResponse<Long>> createRestaurant(
            @PathVariable Long sectionId,
            @Valid @RequestBody AdminRestaurantRequest request
    ) {
        Long restaurantId = adminMountainService.createRestaurant(sectionId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_CREATE_SUCCESS, restaurantId);
    }

    @PutMapping("/restaurants/{restaurantId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateRestaurant(
            @PathVariable Long restaurantId,
            @Valid @RequestBody AdminRestaurantRequest request
    ) {
        adminMountainService.updateRestaurant(restaurantId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_UPDATE_SUCCESS);
    }

    @DeleteMapping("/restaurants/{restaurantId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(@PathVariable Long restaurantId) {
        adminMountainService.deleteRestaurant(restaurantId);
        return ApiResponse.success(SuccessStatus.ADMIN_RESTAURANT_DELETE_SUCCESS);
    }
}
