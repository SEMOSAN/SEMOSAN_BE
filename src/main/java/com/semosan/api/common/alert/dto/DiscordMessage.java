package com.semosan.api.common.alert.dto;

import java.util.List;

public record DiscordMessage(
        String content,
        List<DiscordEmbed> embeds
) {
}
