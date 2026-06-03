package com.semosan.api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kakaoApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    @Bean
    public WebClient appleApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://appleid.apple.com")
                .build();
    }

    @Bean
    public WebClient weatherApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0")
                .build();
    }

}
