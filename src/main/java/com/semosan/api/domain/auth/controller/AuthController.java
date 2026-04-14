package com.semosan.api.domain.auth.controller;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.auth.controller.docs.AuthControllerDocs;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.auth.dto.response.ReissueResponse;
import com.semosan.api.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping("/test/login")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, response);
    }

    @PostMapping("/token/reissue")
    @Override
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String refreshToken = resolveToken(authorizationHeader);
        ReissueResponse response = authService.reissue(refreshToken);
        return ApiResponse.success(SuccessStatus.REISSUE_SUCCESS, response);
    }

    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        return authorizationHeader.substring(7);
    }

}
