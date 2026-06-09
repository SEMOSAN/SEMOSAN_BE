package com.semosan.api.domain.semofeed.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.semofeed.controller.docs.SemoFeedControllerDocs;
import com.semosan.api.domain.semofeed.dto.SemoFeedCreateRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.service.SemoFeedEmojiService;
import com.semosan.api.domain.semofeed.service.SemoFeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/semofeed")
@RequiredArgsConstructor
public class SemoFeedController implements SemoFeedControllerDocs {

    private final SemoFeedService semoFeedService;
    private final SemoFeedEmojiService semoFeedEmojiService;

    @PostMapping
    @Override
    // @RequestBody String 대신 DTO를 사용해 Jackson이 JSON 따옴표를 제거한 뒤 imageUrl을 전달합니다.
    public ResponseEntity<ApiResponse<SemoFeedResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SemoFeedCreateRequest request
    ) {
        SemoFeedResponse response = semoFeedService.create(userId, request.imageUrl());
        return ApiResponse.success(SuccessStatus.SEMOFEED_CREATE_SUCCESS, response);
    }

    @GetMapping
    @Override
    // 공개 세모피드 목록을 작성자 정보와 이모지 상태까지 포함해 조회합니다.
    public ResponseEntity<ApiResponse<PageResponse<SemoFeedResponse>>> listPublic(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 100) Pageable pageable
    ) {
        PageResponse<SemoFeedResponse> response = PageResponse.from(semoFeedService.listPublic(pageable, userId));
        return ApiResponse.success(SuccessStatus.SEMOFEED_LIST_SUCCESS, response);
    }

    @PostMapping("/{semoFeedId}/emojis")
    @Override
    // 세모피드 이모지를 타입별로 등록하거나 취소합니다.
    public ResponseEntity<ApiResponse<SemoFeedEmojiToggleResponse>> toggleEmoji(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long semoFeedId,
            @Valid @RequestBody SemoFeedEmojiRequest request
    ) {
        SemoFeedEmojiToggleResponse response = semoFeedEmojiService.toggleWithCount(userId, semoFeedId, request.emojiType());
        return ApiResponse.success(SuccessStatus.SEMOFEED_EMOJI_TOGGLE_SUCCESS, response);
    }

    @GetMapping("/me")
    @Override
    public ResponseEntity<ApiResponse<List<SemoFeedResponse>>> listMine(
            @AuthenticationPrincipal Long userId
    ) {
        List<SemoFeedResponse> response = semoFeedService.listMine(userId);
        return ApiResponse.success(SuccessStatus.SEMOFEED_MY_LIST_SUCCESS, response);
    }

    @PatchMapping("/{semoFeedId}/public")
    @Override
    public ResponseEntity<ApiResponse<Boolean>> togglePublic(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long semoFeedId
    ) {
        boolean isPublic = semoFeedService.togglePublic(userId, semoFeedId);
        return ApiResponse.success(SuccessStatus.SEMOFEED_TOGGLE_PUBLIC_SUCCESS, isPublic);
    }

    @DeleteMapping("/{semoFeedId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long semoFeedId
    ) {
        semoFeedService.delete(userId, semoFeedId);
        return ApiResponse.success(SuccessStatus.SEMOFEED_DELETE_SUCCESS, null);
    }
}
