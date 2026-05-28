package com.semosan.api.domain.semofeed.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.semofeed.controller.docs.SemoFeedControllerDocs;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.service.SemoFeedEmojiService;
import com.semosan.api.domain.semofeed.service.SemoFeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public ResponseEntity<ApiResponse<SemoFeedResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody String imageUrl
    ) {
        SemoFeedResponse response = semoFeedService.create(userId, imageUrl);
        return ApiResponse.success(SuccessStatus.SEMOFEED_CREATE_SUCCESS, response);
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PageResponse<SemoFeedResponse>>> listPublic(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 100) Pageable pageable
    ) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return ApiResponse.success(
                SuccessStatus.SEMOFEED_LIST_SUCCESS,
                PageResponse.from(semoFeedService.listPublic(pageable, userId))
        );
    }

    @PostMapping("/{semoFeedId}/emojis")
    @Override
    public ResponseEntity<ApiResponse<SemoFeedEmojiToggleResponse>> toggleEmoji(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long semoFeedId,
            @Valid @RequestBody SemoFeedEmojiRequest request
    ) {
        return ApiResponse.success(
                SuccessStatus.SEMOFEED_EMOJI_TOGGLE_SUCCESS,
                semoFeedEmojiService.toggleWithCount(userId, semoFeedId, request.emojiType())
        );
    }

    @GetMapping("/me")
    @Override
    public ResponseEntity<ApiResponse<List<SemoFeedResponse>>> listMine(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(SuccessStatus.SEMOFEED_MY_LIST_SUCCESS, semoFeedService.listMine(userId));
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
