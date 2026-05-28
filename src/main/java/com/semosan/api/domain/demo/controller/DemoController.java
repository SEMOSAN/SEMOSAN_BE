package com.semosan.api.domain.demo.controller;

import com.semosan.api.common.config.DemoProperties;
import com.semosan.api.common.config.MinioProperties;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.demo.controller.docs.DemoControllerDocs;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@EnableConfigurationProperties(DemoProperties.class)
public class DemoController implements DemoControllerDocs {

    private static final String BUCKET = "tracking-photos";

    private final DemoProperties demoProperties;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final TrackingPhotoRepository trackingPhotoRepository;

    @GetMapping("/tracking/sessions/{sessionId}/photos")
    @Override
    public ResponseEntity<ApiResponse<List<String>>> getDemoPhotos(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "3") int count
    ) {
        List<String> shuffled = new ArrayList<>(demoProperties.photoFilenames());
        Collections.shuffle(shuffled);
        List<String> randomUrls = shuffled.subList(0, Math.min(count, shuffled.size()))
                .stream()
                .map(this::presignedGetUrl)
                .toList();

        List<String> uploadedUrls = trackingPhotoRepository
                .findByTrackingSession_IdOrderByMilestoneIndexAsc(sessionId)
                .stream()
                .map(TrackingPhoto::getImageUrl)
                .toList();

        List<String> combined = new ArrayList<>(randomUrls);
        combined.addAll(uploadedUrls);

        return ApiResponse.success(SuccessStatus.TRACKING_PHOTO_LIST_SUCCESS, combined);
    }

    private String presignedGetUrl(String objectKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(BUCKET)
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            return url.replace(minioProperties.endpoint(), minioProperties.publicUrl());
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for {}", objectKey, e);
            return minioProperties.publicUrl() + "/" + BUCKET + "/" + objectKey;
        }
    }
}
