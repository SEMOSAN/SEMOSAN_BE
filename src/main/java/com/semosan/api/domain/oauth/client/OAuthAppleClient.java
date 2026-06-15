package com.semosan.api.domain.oauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAppleClient {

    private static final String APPLE_PUBLIC_KEYS_URI = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final WebClient appleApiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${apple.client-id}")
    private String appleClientId;

    // identity token 검증 후 Claims 반환
    public Claims getAppleClaims(String identityToken) {
        List<Map<String, String>> keys = getApplePublicKeys();
        String kid = extractKid(identityToken);

        Map<String, String> matchedKey = keys.stream()
                .filter(key -> kid.equals(key.get("kid")))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.APPLE_PUBLIC_KEY_NOT_FOUND));

        PublicKey publicKey = buildPublicKey(matchedKey);

        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(appleClientId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (Exception e) {
            log.error("[*] 애플 identity token 검증 실패 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.APPLE_IDENTITY_TOKEN_INVALID);
        }
    }

    // 애플 공개키 목록 조회
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getApplePublicKeys() {
        try {
            Map<String, Object> response = appleApiWebClient.get()
                    .uri(APPLE_PUBLIC_KEYS_URI)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (List<Map<String, String>>) response.get("keys");
        } catch (WebClientResponseException e) {
            log.error("[*] 애플 공개키 조회 실패 status={}", e.getStatusCode());
            throw new GeneralException(ErrorStatus.APPLE_PUBLIC_KEY_REQUEST_FAILED);
        }
    }

    // identity token 헤더에서 kid 추출 — ObjectMapper로 안전하게 파싱
    private String extractKid(String identityToken) {
        try {
            String header = identityToken.split("\\.")[0];
            String decoded = new String(Base64.getUrlDecoder().decode(header));
            return objectMapper.readTree(decoded).get("kid").asText();
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.APPLE_IDENTITY_TOKEN_INVALID);
        }
    }

    // 공개키 맵으로 RSA PublicKey 생성
    private PublicKey buildPublicKey(Map<String, String> key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.get("n"));
            byte[] eBytes = Base64.getUrlDecoder().decode(key.get("e"));

            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, nBytes),
                    new BigInteger(1, eBytes)
            );
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            log.error("[*] 애플 공개키 생성 실패 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.APPLE_PUBLIC_KEY_NOT_FOUND);
        }
    }

}
