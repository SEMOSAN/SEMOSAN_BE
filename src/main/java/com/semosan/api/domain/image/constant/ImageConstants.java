package com.semosan.api.domain.image.constant;

import java.util.Map;
import java.util.Set;

public final class ImageConstants {

    private ImageConstants() {}

    public static final Set<String> ALLOWED_BUCKETS = Set.of("reviews", "mountains", "restaurants", "posts", "semofeed", "user", "tracking-photos");
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    public static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp"
    );
}
