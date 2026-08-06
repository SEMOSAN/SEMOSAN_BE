package com.semosan.api.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class FirebaseConfigTest {

    @Test
    void initializeSkipsWhenFirebaseAppAlreadyExists() throws Exception {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "serviceAccountPath", "missing.json");
        FirebaseApp existingApp = mock(FirebaseApp.class);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(existingApp));

            config.initialize();

            firebaseApp.verify(FirebaseApp::getApps);
            firebaseApp.verifyNoMoreInteractions();
        }
    }

    @Test
    void initializeUsesFileSystemServiceAccountWhenFileExists() throws Exception {
        Path serviceAccount = Files.createTempFile("firebase-service-account", ".json");
        Files.writeString(serviceAccount, "{}");
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "serviceAccountPath", serviceAccount.toString());
        GoogleCredentials credentials = mock(GoogleCredentials.class);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<GoogleCredentials> googleCredentials = mockStatic(GoogleCredentials.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);
            firebaseApp.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class))).thenReturn(mock(FirebaseApp.class));

            config.initialize();

            googleCredentials.verify(() -> GoogleCredentials.fromStream(any(InputStream.class)));
            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)));
        }
    }
}
