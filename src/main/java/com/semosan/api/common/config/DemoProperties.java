package com.semosan.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "demo")
public record DemoProperties(
        List<String> photoFilenames
) {}
