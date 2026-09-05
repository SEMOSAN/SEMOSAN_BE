package com.semosan.api.common.jwt;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";
    public static final String TOKEN_TYPE_ADMIN = "ADMIN";

    private final SecretKey secretKey;
    @Getter
    private final long accessTokenExpiration;
    @Getter
    private final long refreshTokenExpiration;
    private final TokenRedisService tokenRedisService;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            TokenRedisService tokenRedisService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.tokenRedisService = tokenRedisService;
    }

    public TokenIssuance issueTokens(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        tokenRedisService.saveRefreshToken(user.getId(), hashToken(refreshToken), refreshTokenExpiration);
        return new TokenIssuance(accessToken, refreshToken);
    }

    // Access Token 생성
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("loginType", user.getOauthProvider().name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Refresh Token을 SHA-256으로 해시 — DB 저장 전 사용
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralException(ErrorStatus.JWT_GENERAL_ERROR);
        }
    }

    public String generateAdminAccessToken(Admin admin) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(admin.getId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ADMIN)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Access Token 검증 후 Claims를 반환해 호출부에서 같은 토큰을 다시 파싱하지 않도록 한다.
    // tokenType 검증으로 refresh 토큰이 access 로 통용되는 것을 차단한다.
    public Claims validateAccessTokenAndGetClaims(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        Claims claims = parseClaims(accessToken);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType) && !TOKEN_TYPE_ADMIN.equals(tokenType)) {
            throw new GeneralException(ErrorStatus.JWT_INVALID_TYPE);
        }
        return claims;
    }

    // Refresh Token 서명/만료 검증 후 Claims 반환 — DB 비교 전 1차 검증용.
    // claim 없는 기존 refresh 토큰은 만료 전까지 허용해 강제 로그아웃을 막는다.
    public Claims validateRefreshTokenSignature(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        Claims claims = parseClaims(refreshToken);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (tokenType != null && !TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new GeneralException(ErrorStatus.JWT_INVALID_TYPE);
        }
        return claims;
    }

    public void validateRefreshToken(String refreshToken, Long userId) {
        String storedHash = tokenRedisService.getRefreshToken(userId);
        if (storedHash == null)
            throw new GeneralException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
        if (!hashToken(refreshToken).equals(storedHash))
            throw new GeneralException(ErrorStatus.REFRESH_TOKEN_MISMATCH);
    }

    public void blacklistAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);
        long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingMs > 0) {
            tokenRedisService.addToBlacklist(accessToken, remainingMs);
        }
    }

    public boolean isAccessTokenBlacklisted(String accessToken) {
        return tokenRedisService.isBlacklisted(accessToken);
    }

    public void deleteRefreshToken(Long userId) {
        tokenRedisService.deleteRefreshToken(userId);
    }

    public Long getUserIdFromClaims(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.JWT_EXTRACT_ID_FAILED);
        }
    }

    // 내부 Claims 파싱 로직
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SecurityException e) {
            throw new GeneralException(ErrorStatus.JWT_INVALID_SIGNATURE);
        } catch (MalformedJwtException e) {
            throw new GeneralException(ErrorStatus.JWT_MALFORMED);
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.JWT_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new GeneralException(ErrorStatus.JWT_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.JWT_INVALID);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.JWT_GENERAL_ERROR);
        }
    }

}
