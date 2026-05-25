package com.semosan.api.domain.appversion.dto.response;

public record AppVersionResponse(
        PlatformVersion ios,
        PlatformVersion android,
        String updatedAt
) {
    public record PlatformVersion(
            String latestVersion,
            String minimumVersion,
            boolean forceUpdate,
            String storeUrl,
            String releaseNotes,
            boolean maintenanceMode,
            String maintenanceMessage
    ) {
    }
}
