package com.semosan.api.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingPropertiesTest {

    @Test
    void defaultStreamMaxLenIsOneHundredThousand() {
        TrackingProperties properties = new TrackingProperties();

        assertThat(properties.getStreamMaxLen()).isEqualTo(100_000L);
    }

    @Test
    void settersAndGettersExposeConfiguredValues() {
        TrackingProperties properties = new TrackingProperties();

        properties.setStreamKey("tracking:gps");
        properties.setConsumerGroup("tracking-group");
        properties.setStreamMaxLen(500L);

        assertThat(properties.getStreamKey()).isEqualTo("tracking:gps");
        assertThat(properties.getConsumerGroup()).isEqualTo("tracking-group");
        assertThat(properties.getStreamMaxLen()).isEqualTo(500L);
    }
}
