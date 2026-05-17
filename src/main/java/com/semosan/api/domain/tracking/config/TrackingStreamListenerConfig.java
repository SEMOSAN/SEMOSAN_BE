package com.semosan.api.domain.tracking.config;

import com.semosan.api.common.config.TrackingProperties;
import com.semosan.api.domain.tracking.service.TrackingStreamConsumer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;

/**
 * Redis Stream(tracking:gps) 의 GPS 점을 소비하는 컨테이너를 부트업한다.
 *  - Consumer group 자체는 RedisStreamConfig 에서 사전 생성됨.
 *  - 본 클래스는 그룹의 이 인스턴스를 위한 consumer 등록 + listener container 시작.
 *  - consumer 이름은 호스트명 + UUID prefix 로 인스턴스별 유일성 확보 (다중 인스턴스 대비).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TrackingStreamListenerConfig implements InitializingBean {

    private final RedisConnectionFactory redisConnectionFactory;
    private final TrackingProperties trackingProperties;
    private final TrackingStreamConsumer trackingStreamConsumer;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    @Override
    public void afterPropertiesSet() {
        var options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.receiveAutoAck(
                Consumer.from(trackingProperties.getConsumerGroup(), buildConsumerName()),
                StreamOffset.create(trackingProperties.getStreamKey(), ReadOffset.lastConsumed()),
                trackingStreamConsumer
        );
        container.start();
        log.info("Started Redis Stream listener: stream={} group={}",
                trackingProperties.getStreamKey(),
                trackingProperties.getConsumerGroup());
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private static String buildConsumerName() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown";
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
