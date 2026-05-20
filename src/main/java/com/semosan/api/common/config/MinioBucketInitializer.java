package com.semosan.api.common.config;

import com.semosan.api.domain.image.constant.ImageConstants;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;

    @Override
    public void run(ApplicationArguments args) {
        for (String bucket : ImageConstants.ALLOWED_BUCKETS) {
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO 버킷 생성: {}", bucket);
                }
            } catch (Exception e) {
                log.warn("MinIO 버킷 초기화 실패: {} - {}", bucket, e.getMessage());
            }
        }
    }
}
