package com.semosan.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tracking")
public class TrackingProperties {

    private String streamKey;
    private String consumerGroup;
    private long streamMaxLen = 100_000;
}
