package com.semosan.api.domain.tracking.config;

import com.semosan.api.common.config.TrackingProperties;
import com.semosan.api.domain.tracking.service.TrackingStreamConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrackingStreamListenerConfigTest {

    @Test
    void stopDoesNothingWhenContainerIsNull() {
        TrackingStreamListenerConfig config = new TrackingStreamListenerConfig(
                mock(RedisConnectionFactory.class),
                mock(TrackingProperties.class),
                mock(TrackingStreamConsumer.class)
        );

        assertThatCode(config::stop).doesNotThrowAnyException();
    }

    @Test
    void stopStopsContainerWhenContainerExists() {
        TrackingStreamListenerConfig config = new TrackingStreamListenerConfig(
                mock(RedisConnectionFactory.class),
                mock(TrackingProperties.class),
                mock(TrackingStreamConsumer.class)
        );
        @SuppressWarnings("unchecked")
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                mock(StreamMessageListenerContainer.class);
        ReflectionTestUtils.setField(config, "container", container);

        config.stop();

        verify(container).stop();
    }
}
