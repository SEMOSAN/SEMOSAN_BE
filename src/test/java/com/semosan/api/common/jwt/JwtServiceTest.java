package com.semosan.api.common.jwt;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";
    private static final long ACCESS_EXPIRATION = 60_000L;
    private static final long REFRESH_EXPIRATION = 120_000L;

    @Mock
    private TokenRedisService tokenRedisService;

    @Test
    void issueTokensSavesHashedRefreshTokenAndReturnsTokens() {
        JwtService jwtService = jwtService();
        User user = user(1L);

        TokenIssuance tokens = jwtService.issueTokens(user);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        verify(tokenRedisService).saveRefreshToken(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(jwtService.hashToken(tokens.refreshToken())),
                org.mockito.ArgumentMatchers.eq(REFRESH_EXPIRATION)
        );
    }

    @Test
    void generateAccessTokenIncludesUserIdAndLoginType() {
        JwtService jwtService = jwtService();

        Claims claims = jwtService.validateAccessTokenAndGetClaims(jwtService.generateAccessToken(user(2L)));

        assertThat(claims.getSubject()).isEqualTo("2");
        assertThat(claims.get("loginType", String.class)).isEqualTo("TEST");
    }

    @Test
    void generateAdminAccessTokenIncludesAdminTokenType() {
        JwtService jwtService = jwtService();
        Admin admin = Admin.create("admin", "password", "관리자");
        ReflectionTestUtils.setField(admin, "id", 9L);

        Claims claims = jwtService.validateAccessTokenAndGetClaims(jwtService.generateAdminAccessToken(admin));

        assertThat(claims.getSubject()).isEqualTo("9");
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void generateAccessTokenIncludesAccessTokenType() {
        JwtService jwtService = jwtService();

        Claims claims = jwtService.validateAccessTokenAndGetClaims(jwtService.generateAccessToken(user(2L)));

        assertThat(jwtService.getTokenType(claims)).isEqualTo(TokenType.ACCESS);
    }

    @Test
    void validateAccessTokenThrowsWhenRefreshTokenIsUsedAsAccessToken() {
        JwtService jwtService = jwtService();
        String refreshToken = jwtService.generateRefreshToken(user(1L));

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(refreshToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
    }

    @Test
    void validateAccessTokenThrowsWhenTokenTypeClaimIsMissing() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String legacyToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(legacyToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
    }

    @Test
    void validateRefreshTokenSignatureThrowsWhenAccessTokenIsUsedAsRefreshToken() {
        JwtService jwtService = jwtService();
        String accessToken = jwtService.generateAccessToken(user(1L));

        assertThatThrownBy(() -> jwtService.validateRefreshTokenSignature(accessToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
    }

    @Test
    void validateRefreshTokenSignatureAllowsLegacyRefreshTokenWithoutTokenType() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String legacyRefreshToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        Claims claims = jwtService.validateRefreshTokenSignature(legacyRefreshToken);

        assertThat(claims.getSubject()).isEqualTo("1");
    }

    @Test
    void validateRefreshTokenSignatureThrowsWhenTokenTypeIsUnknown() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String unknownTypeToken = Jwts.builder()
                .subject("1")
                .claim("tokenType", "UNKNOWN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateRefreshTokenSignature(unknownTypeToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
    }

    @Test
    void validateAccessTokenThrowsWhenTokenTypeIsUnknown() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String unknownTypeToken = Jwts.builder()
                .subject("1")
                .claim("tokenType", "UNKNOWN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(unknownTypeToken))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
    }

    @Test
    void validateAccessTokenThrowsWhenTokenIsBlank() {
        JwtService jwtService = jwtService();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(" "))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_TOKEN_NOT_FOUND);
    }

    @Test
    void validateRefreshTokenSignatureThrowsWhenTokenIsNull() {
        JwtService jwtService = jwtService();

        assertThatThrownBy(() -> jwtService.validateRefreshTokenSignature(null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_TOKEN_NOT_FOUND);
    }

    @Test
    void validateRefreshTokenPassesWhenStoredHashMatches() {
        JwtService jwtService = jwtService();
        String refreshToken = jwtService.generateRefreshToken(user(1L));
        when(tokenRedisService.getRefreshToken(1L)).thenReturn(jwtService.hashToken(refreshToken));

        jwtService.validateRefreshToken(refreshToken, 1L);
    }

    @Test
    void validateRefreshTokenThrowsWhenStoredHashIsMissing() {
        JwtService jwtService = jwtService();
        when(tokenRedisService.getRefreshToken(1L)).thenReturn(null);

        assertThatThrownBy(() -> jwtService.validateRefreshToken("refresh", 1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void validateRefreshTokenThrowsWhenStoredHashDoesNotMatch() {
        JwtService jwtService = jwtService();
        when(tokenRedisService.getRefreshToken(1L)).thenReturn("different");

        assertThatThrownBy(() -> jwtService.validateRefreshToken("refresh", 1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.REFRESH_TOKEN_MISMATCH);
    }

    @Test
    void blacklistAccessTokenAddsTokenWhenItHasRemainingTime() {
        JwtService jwtService = jwtService();
        String accessToken = jwtService.generateAccessToken(user(1L));

        jwtService.blacklistAccessToken(accessToken);

        verify(tokenRedisService).addToBlacklist(
                org.mockito.ArgumentMatchers.eq(accessToken),
                org.mockito.ArgumentMatchers.longThat(remaining -> remaining > 0 && remaining <= ACCESS_EXPIRATION)
        );
    }

    @Test
    void isAccessTokenBlacklistedDelegatesToRedisService() {
        JwtService jwtService = jwtService();
        when(tokenRedisService.isBlacklisted("access")).thenReturn(true);

        assertThat(jwtService.isAccessTokenBlacklisted("access")).isTrue();
    }

    @Test
    void deleteRefreshTokenDelegatesToRedisService() {
        JwtService jwtService = jwtService();

        jwtService.deleteRefreshToken(1L);

        verify(tokenRedisService).deleteRefreshToken(1L);
    }

    @Test
    void getUserIdFromClaimsParsesSubject() {
        JwtService jwtService = jwtService();
        Claims claims = jwtService.validateAccessTokenAndGetClaims(jwtService.generateAccessToken(user(3L)));

        Long userId = jwtService.getUserIdFromClaims(claims);

        assertThat(userId).isEqualTo(3L);
    }

    @Test
    void getUserIdFromClaimsThrowsWhenSubjectIsInvalid() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("not-number")
                .claim("tokenType", TokenType.ACCESS.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        Claims claims = jwtService.validateAccessTokenAndGetClaims(token);

        assertThatThrownBy(() -> jwtService.getUserIdFromClaims(claims))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_EXTRACT_ID_FAILED);
    }

    @Test
    void validateAccessTokenThrowsWhenTokenIsMalformed() {
        JwtService jwtService = jwtService();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims("malformed.token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_MALFORMED);
    }

    @Test
    void validateAccessTokenThrowsWhenSignatureIsInvalid() {
        JwtService jwtService = jwtService();
        SecretKey otherKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8)
        );
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(token))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_GENERAL_ERROR);
    }

    @Test
    void validateAccessTokenThrowsWhenTokenIsExpired() {
        JwtService jwtService = jwtService();
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 2_000L))
                .expiration(new Date(System.currentTimeMillis() - 1_000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetClaims(token))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_EXPIRED);
    }

    private JwtService jwtService() {
        return new JwtService(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION, tokenRedisService);
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
