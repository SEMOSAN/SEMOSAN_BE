package com.semosan.api.common.alert.dto;

public record DiscordEmbed(
        String title,
        String description,
        Integer color
) {
}
