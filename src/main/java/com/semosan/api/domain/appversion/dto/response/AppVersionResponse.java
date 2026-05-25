package com.semosan.api.domain.appversion.dto.response;

public record AppVersionResponse(
        String minimumVersion,
        String latestVersion,
        String updateUrl,
        String message
) {
}