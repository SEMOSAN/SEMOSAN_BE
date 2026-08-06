package com.semosan.api;

import com.semosan.api.domain.oauth.properties.KakaoProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.profiles.active=test"};

            ApiApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ApiApplication.class, args));
        }
    }

    @Test
    void applicationDeclaresRequiredSpringAnnotations() {
        assertThat(ApiApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(ApiApplication.class.isAnnotationPresent(EnableJpaAuditing.class)).isTrue();
        assertThat(ApiApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();

        EnableConfigurationProperties properties =
                ApiApplication.class.getAnnotation(EnableConfigurationProperties.class);
        assertThat(properties.value()).contains(KakaoProperties.class);
    }

}
