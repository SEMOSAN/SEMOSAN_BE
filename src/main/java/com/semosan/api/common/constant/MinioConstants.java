package com.semosan.api.common.constant;

import java.util.Map;
import java.util.Set;

public final class MinioConstants {

    private MinioConstants() {}

    public static final String APP_CONFIG_BUCKET = "app-config";

    public static final Set<String> REQUIRED_BUCKETS = Set.of(
            "reviews", "mountains", "restaurants", "posts",
            "semofeed", "user", "tracking-photos", APP_CONFIG_BUCKET
    );

    public static final Set<String> ALLOWED_IMAGE_BUCKETS = Set.of(
            "reviews", "mountains", "restaurants", "posts",
            "semofeed", "user", "tracking-photos"
    );

    /** 허용 확장자와 서명에 실을 Content-Type 의 단일 출처. 확장자 추가는 여기만 고치면 된다. */
    public static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp"
    );

    public static final Set<String> ALLOWED_EXTENSIONS = CONTENT_TYPE_MAP.keySet();
}
