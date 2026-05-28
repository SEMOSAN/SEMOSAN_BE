
package com.semosan.api.domain.demo.service;

import com.semosan.api.common.config.DemoProperties;
import com.semosan.api.common.config.MinioProperties;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemoService {

    private static final String BUCKET = "tracking-photos";

    private final DemoProperties demoProperties;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final TrackingPhotoRepository trackingPhotoRepository;

    public List<String> getDemoPhotos(Long sessionId, int count) {
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
        return combined;
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
