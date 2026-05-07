package com.semosan.api.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    // 이 빈 만들어진 직후에 이 메서드 자동 실행, 앱 시작할 때 딱 한 번 실행됨
    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialized, skipping");
            return;
        }

        // new File() 안 쓰는 이유는 jar로 빌드하면 파일들이 jar 안에 들어가는데, new File()은 jar 내부 파일을 못 읽음
        // ClassPathResource는 잘 읽는다고 함
        try (InputStream serviceAccount = new ClassPathResource(serviceAccountPath).getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp initialized successfully");
        }
    }

}