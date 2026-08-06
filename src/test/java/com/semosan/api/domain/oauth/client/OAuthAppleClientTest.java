package com.semosan.api.domain.oauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthAppleClientTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getAppleClaimsThrowsInvalidTokenWhenHeaderCannotBeParsed() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthAppleClient client = new OAuthAppleClient(webClient, new ObjectMapper());

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("https://appleid.apple.com/auth/keys")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("keys", List.of())));

        assertThatThrownBy(() -> client.getAppleClaims("invalid-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APPLE_IDENTITY_TOKEN_INVALID);
    }

    @Test
    void extractKidReadsKidFromJwtHeader() {
        OAuthAppleClient client = new OAuthAppleClient(mock(WebClient.class), new ObjectMapper());
        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"kid\":\"apple-key\"}".getBytes());

        String result = ReflectionTestUtils.invokeMethod(client, "extractKid", header + ".payload.signature");

        assertThat(result).isEqualTo("apple-key");
    }

    @Test
    void buildPublicKeyCreatesRsaPublicKey() throws Exception {
        OAuthAppleClient client = new OAuthAppleClient(mock(WebClient.class), new ObjectMapper());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        RSAPublicKey publicKey = (RSAPublicKey) generator.generateKeyPair().getPublic();
        Map<String, String> key = Map.of(
                "n", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray()),
                "e", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray())
        );

        PublicKey result = ReflectionTestUtils.invokeMethod(client, "buildPublicKey", key);

        assertThat(result.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void buildPublicKeyThrowsWhenKeyMaterialIsInvalid() {
        OAuthAppleClient client = new OAuthAppleClient(mock(WebClient.class), new ObjectMapper());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                client,
                "buildPublicKey",
                Map.of("n", "not-base64", "e", "AQAB")
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.APPLE_PUBLIC_KEY_NOT_FOUND);
    }
}
