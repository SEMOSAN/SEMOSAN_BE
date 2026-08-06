package com.semosan.api.common.config;

import com.semosan.api.common.constant.MinioConstants;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioBucketInitializerTest {

    @Test
    void runCreatesMissingBuckets() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MinioBucketInitializer initializer = new MinioBucketInitializer(minioClient);

        initializer.run(null);

        verify(minioClient, times(MinioConstants.REQUIRED_BUCKETS.size())).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(MinioConstants.REQUIRED_BUCKETS.size())).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void runDoesNotCreateExistingBuckets() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        MinioBucketInitializer initializer = new MinioBucketInitializer(minioClient);

        initializer.run(null);

        verify(minioClient, times(MinioConstants.REQUIRED_BUCKETS.size())).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void runSwallowsBucketInitializationFailureAndContinues() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("minio down"))
                .thenReturn(true);
        MinioBucketInitializer initializer = new MinioBucketInitializer(minioClient);

        initializer.run(null);

        verify(minioClient, times(MinioConstants.REQUIRED_BUCKETS.size())).bucketExists(any(BucketExistsArgs.class));
    }
}
