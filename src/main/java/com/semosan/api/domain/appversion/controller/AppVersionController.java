package com.semosan.api.domain.appversion.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.appversion.controller.docs.AppVersionControllerDocs;
import com.semosan.api.domain.appversion.dto.request.UpdateAppVersionRequest;
import com.semosan.api.domain.appversion.dto.response.AppVersionResponse;
import com.semosan.api.domain.appversion.service.AppVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-version")
@RequiredArgsConstructor
public class AppVersionController implements AppVersionControllerDocs {

    private final AppVersionService appVersionService;

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion() {
        AppVersionResponse response = appVersionService.getAppVersion();
        return ApiResponse.success(SuccessStatus.APP_VERSION_GET_SUCCESS, response);
    }

    @PutMapping
    @Override
    public ResponseEntity<ApiResponse<AppVersionResponse>> updateAppVersion(
            @Valid @RequestBody UpdateAppVersionRequest request
    ) {
        AppVersionResponse response = appVersionService.updateAppVersion(request);
        return ApiResponse.success(SuccessStatus.APP_VERSION_UPDATE_SUCCESS, response);
    }
}
