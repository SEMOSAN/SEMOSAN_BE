package com.semosan.api.domain.demo.controller;

import com.semosan.api.common.config.DemoProperties;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.demo.controller.docs.DemoControllerDocs;
import com.semosan.api.domain.demo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@EnableConfigurationProperties(DemoProperties.class)
public class DemoController implements DemoControllerDocs {

    private final DemoService demoService;

    @GetMapping("/tracking/sessions/{sessionId}/photos")
    @Override
    public ResponseEntity<ApiResponse<List<String>>> getDemoPhotos(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "3") int count
    ) {
        List<String> combined = demoService.getDemoPhotos(sessionId, count);
        return ApiResponse.success(SuccessStatus.TRACKING_PHOTO_LIST_SUCCESS, combined);
    }
}
