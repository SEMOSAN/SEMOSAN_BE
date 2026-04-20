package com.semosan.api.domain.oauth.client;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.domain.oauth.properties.KakaoProperties;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.oauth.dto.KakaoTokenResponse;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthKakaoClient {

    private final WebClient kakaoAuthWebClient;
    private final WebClient kakaoApiWebClient;
    private final KakaoProperties kakaoProperties;

    // 인가 코드 -> 카카오 액세스 토큰
    public KakaoTokenResponse getKakaoToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoProperties.clientId());
        form.add("redirect_uri", kakaoProperties.redirectUri());
        form.add("code", code);
        if (StringUtils.hasText(kakaoProperties.clientSecret())) {
            form.add("client_secret", kakaoProperties.clientSecret());
        }

        try {
            KakaoTokenResponse response = kakaoAuthWebClient.post()
                    .uri("/oauth/token")
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            r -> r.bodyToMono(String.class)
                                    .flatMap(body -> handleError(r.statusCode(), body, "토큰 발급"))
                    )
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
            }
            return response;
        } catch (WebClientResponseException e) {
            log.error("[*] 카카오 토큰 발급 실패 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
        }
    }

    // 카카오 액세스 토큰 -> 사용자 정보
    public KakaoUserInfoResponse getKakaoUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = kakaoApiWebClient.get()
                    .uri("/v2/user/me")
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            r -> r.bodyToMono(String.class)
                                    .flatMap(body -> handleError(r.statusCode(), body, "사용자 정보 조회"))
                    )
                    .bodyToMono(KakaoUserInfoResponse.class)
                    .block();

            if (response == null || response.id() == null) {
                throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
            }
            return response;
        } catch (WebClientResponseException e) {
            log.error("[*] 카카오 사용자 정보 조회 실패 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
        }
    }

    // 카카오 연동 해제
    public void unlinkKakaoUser(String kakaoOauthId) {
        try {
            kakaoApiWebClient.post()
                    .uri("/v1/user/unlink")
                    .header("Authorization", "KakaoAK " + kakaoProperties.adminKey())
                    .body(BodyInserters.fromFormData("target_id_type", "user_id")
                            .with("target_id", kakaoOauthId))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            r -> r.bodyToMono(String.class)
                                    .flatMap(body -> handleError(r.statusCode(), body, "연동 해제"))
                    )
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[*] 카카오 연동 해제 실패 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(ErrorStatus.KAKAO_UNLINK_FAILED);
        }
    }

    private Mono<Throwable> handleError(HttpStatusCode statusCode, String body, String action) {
        log.error("[*] 카카오 {} 실패 status={}, body={}", action, statusCode, body);
        return Mono.error(new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED));
    }

}
