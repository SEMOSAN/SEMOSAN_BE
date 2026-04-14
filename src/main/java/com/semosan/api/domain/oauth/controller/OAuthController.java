package com.semosan.api.domain.oauth.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.oauth.controller.docs.OAuthControllerDocs;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.oauth.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController implements OAuthControllerDocs {

    private final OAuthService oAuthService;

    @PostMapping("/kakao/login")
    @Override
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> kakaoLogin(
            @Valid @RequestBody OAuthKakaoLoginRequest request
    ) {
        OAuthLoginResponse response = oAuthService.kakaoLogin(request);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, response);
    }

    @PostMapping("/apple/login")
    @Override
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> appleLogin(
            @Valid @RequestBody OAuthAppleLoginRequest request
    ) {
        OAuthLoginResponse response = oAuthService.appleLogin(request);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, response);
    }

}
