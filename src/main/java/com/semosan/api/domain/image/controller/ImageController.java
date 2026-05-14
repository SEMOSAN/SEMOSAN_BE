package com.semosan.api.domain.image.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.image.controller.docs.ImageControllerDocs;
import com.semosan.api.domain.image.dto.response.PresignedUrlResponse;
import com.semosan.api.domain.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController implements ImageControllerDocs {

    private final ImageService imageService;

    @GetMapping("/presigned-url")
    @Override
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String bucket,
            @RequestParam String filename
    ) {
        PresignedUrlResponse response = imageService.generatePresignedUrl(bucket, filename);
        return ApiResponse.success(SuccessStatus.PRESIGNED_URL_SUCCESS, response);
    }
}
