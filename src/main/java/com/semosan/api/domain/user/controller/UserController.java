package com.semosan.api.domain.user.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.user.controller.docs.UserControllerDocs;
import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.service.UserOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserOnboardingService userOnboardingService;

    // 로그인한 사용자의 온보딩 정보를 등록합니다.
    @PostMapping("/onboarding")
    @Override
    public ResponseEntity<ApiResponse<Void>> registerUserOnboarding(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterOnboardingRequest request
    ) {
        userOnboardingService.registerUserOnboarding(userId, request);
        return ApiResponse.success(SuccessStatus.ONBOARDING_REGISTER_SUCCESS);
    }
}
