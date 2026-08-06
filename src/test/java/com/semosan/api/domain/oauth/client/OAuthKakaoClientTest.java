package com.semosan.api.domain.oauth.client;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.oauth.properties.KakaoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthKakaoClientTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getKakaoUserInfoReturnsResponse() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(123L, null);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/v2/user/me")).thenReturn(headersSpec);
        when(headersSpec.header("Authorization", "Bearer access-token")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserInfoResponse.class)).thenReturn(Mono.just(userInfo));

        KakaoUserInfoResponse result = client.getKakaoUserInfo("access-token");

        assertThat(result).isSameAs(userInfo);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getKakaoUserInfoThrowsWhenResponseIsEmpty() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/v2/user/me")).thenReturn(headersSpec);
        when(headersSpec.header("Authorization", "Bearer access-token")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserInfoResponse.class)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> client.getKakaoUserInfo("access-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getKakaoUserInfoThrowsWhenResponseIdIsNull() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/v2/user/me")).thenReturn(headersSpec);
        when(headersSpec.header("Authorization", "Bearer access-token")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoUserInfoResponse.class))
                .thenReturn(Mono.just(new KakaoUserInfoResponse(null, null)));

        assertThatThrownBy(() -> client.getKakaoUserInfo("access-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getKakaoUserInfoWrapsWebClientResponseException() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/v2/user/me")).thenReturn(headersSpec);
        when(headersSpec.header("Authorization", "Bearer access-token")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenThrow(WebClientResponseException.create(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                null,
                "error".getBytes(),
                null
        ));

        assertThatThrownBy(() -> client.getKakaoUserInfo("access-token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void unlinkKakaoUserCompletesRequest() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/v1/user/unlink")).thenReturn(bodySpec);
        when(bodySpec.header("Authorization", "KakaoAK admin-key")).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserter.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        assertThatCode(() -> client.unlinkKakaoUser("123"))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void unlinkKakaoUserWrapsWebClientResponseException() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        OAuthKakaoClient client = new OAuthKakaoClient(webClient, kakaoProperties());

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/v1/user/unlink")).thenReturn(bodySpec);
        when(bodySpec.header("Authorization", "KakaoAK admin-key")).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserter.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenThrow(WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                null,
                "error".getBytes(),
                null
        ));

        assertThatThrownBy(() -> client.unlinkKakaoUser("123"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.KAKAO_UNLINK_FAILED);
    }

    @Test
    void handleErrorReturnsKakaoTokenRequestFailure() {
        OAuthKakaoClient client = new OAuthKakaoClient(mock(WebClient.class), kakaoProperties());

        @SuppressWarnings("unchecked")
        Mono<Throwable> result = ReflectionTestUtils.invokeMethod(
                client,
                "handleError",
                HttpStatus.BAD_REQUEST,
                "error-body",
                "사용자 정보 조회"
        );

        assertThatThrownBy(result::block)
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
    }

    private KakaoProperties kakaoProperties() {
        return new KakaoProperties("client-id", "redirect-uri", "client-secret", "admin-key");
    }
}
