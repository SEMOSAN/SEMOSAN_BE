package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.controller.docs.MountainControllerDocs;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.service.MountainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mountains")
@RequiredArgsConstructor
public class MountainController implements MountainControllerDocs {

    private final MountainService mountainService;

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<List<MountainListResponse>>> getMountains() {
        List<MountainListResponse> response = mountainService.getMountains();
        return ApiResponse.success(SuccessStatus.MOUNTAIN_LIST_SUCCESS, response);
    }

    @GetMapping("/search")
    @Override
    public ResponseEntity<ApiResponse<List<MountainListResponse>>> searchMountains(
            @RequestParam String keyword
    ) {
        List<MountainListResponse> response = mountainService.searchMountains(keyword);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_SEARCH_SUCCESS, response);
    }

    @GetMapping("/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<MountainDetailResponse>> getMountainDetail(
            @PathVariable Long mountainId
    ) {
        MountainDetailResponse response = mountainService.getMountainDetail(mountainId);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_DETAIL_SUCCESS, response);
    }
}
