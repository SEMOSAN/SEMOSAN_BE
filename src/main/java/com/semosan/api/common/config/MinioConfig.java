package com.semosan.api.common.config;

import com.semosan.api.domain.image.constant.ImageConstants;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private final MinioProperties properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @PostConstruct
    public void initBuckets() {
        MinioClient client = minioClient();
        for (String bucket : ImageConstants.ALLOWED_BUCKETS) {
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO 버킷 생성: {}", bucket);
                }
            } catch (Exception e) {
                log.warn("MinIO 버킷 초기화 실패: {} - {}", bucket, e.getMessage());
            }
        }
    }
}