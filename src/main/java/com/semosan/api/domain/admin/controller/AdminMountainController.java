package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.controller.docs.AdminMountainControllerDocs;
import com.semosan.api.domain.admin.dto.request.AdminCourseSummitRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.admin.dto.request.AdminTransportationRequest;
import com.semosan.api.domain.admin.dto.response.AdminCourseWaypointsResponse;
import com.semosan.api.domain.admin.dto.response.AdminMountainListResponse;
import com.semosan.api.domain.admin.service.AdminMountainService;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMountainController implements AdminMountainControllerDocs {

    private final AdminMountainService adminMountainService;

    @GetMapping("/mountains")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<AdminMountainListResponse>>> getMountains(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        PageResponse<AdminMountainListResponse> response = PageResponse.from(
                adminMountainService.getMountains(keyword, visibility, pageable)
        );
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_LIST_SUCCESS, response);
    }

    @GetMapping("/mountains/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<MountainDetailResponse>> getMountainDetail(
            @PathVariable Long mountainId
    ) {
        MountainDetailResponse response = adminMountainService.getMountainDetail(mountainId);
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_DETAIL_SUCCESS, response);
    }

    @PutMapping("/mountains/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateMountain(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminMountainUpdateRequest request
    ) {
        adminMountainService.updateMountain(mountainId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_UPDATE_SUCCESS);
    }

    @GetMapping("/mountains/{mountainId}/waypoints")
    @Override
    public ResponseEntity<ApiResponse<List<AdminCourseWaypointsResponse>>> getWaypoints(
            @PathVariable Long mountainId
    ) {
        List<AdminCourseWaypointsResponse> response = adminMountainService.getWaypoints(mountainId);
        return ApiResponse.success(SuccessStatus.ADMIN_MOUNTAIN_WAYPOINT_LIST_SUCCESS, response);
    }

    @PatchMapping("/courses/{courseId}/summit")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateSummit(
            @PathVariable Long courseId,
            @Valid @RequestBody AdminCourseSummitRequest request
    ) {
        adminMountainService.updateSummit(courseId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_COURSE_SUMMIT_UPDATE_SUCCESS);
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

    @PostMapping("/mountains/{mountainId}/transportations")
    @Override
    public ResponseEntity<ApiResponse<Long>> createTransportation(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminTransportationRequest request
    ) {
        Long transportationId = adminMountainService.createTransportation(mountainId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_TRANSPORTATION_CREATE_SUCCESS, transportationId);
    }

    @PutMapping("/transportations/{transportationId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateTransportation(
            @PathVariable Long transportationId,
            @Valid @RequestBody AdminTransportationRequest request
    ) {
        adminMountainService.updateTransportation(transportationId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_TRANSPORTATION_UPDATE_SUCCESS);
    }

    @DeleteMapping("/transportations/{transportationId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteTransportation(@PathVariable Long transportationId) {
        adminMountainService.deleteTransportation(transportationId);
        return ApiResponse.success(SuccessStatus.ADMIN_TRANSPORTATION_DELETE_SUCCESS);
    }
}
