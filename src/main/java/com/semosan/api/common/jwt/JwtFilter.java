package com.semosan.api.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.config.SecurityConfig;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    // SecurityConfig의 permitAll 경로와 동일하게 맞춰줍니다.
    // WebSocket 핸드셰이크(/ws/tracking)는 HTTP JWT 필터 우회 — STOMP CONNECT 프레임의
    // StompAuthChannelInterceptor 가 별도로 인증한다.
    private static final List<PathPatternRequestMatcher> EXCLUDED_PATHS =
            Stream.of(
                            SecurityConfig.SWAGGER_URIS,
                            SecurityConfig.OAUTH_URIS,
                            SecurityConfig.AUTH_URIS,
                            SecurityConfig.WEBSOCKET_URIS,
                            SecurityConfig.ADMIN_PUBLIC_URIS
                    )
                    .flatMap(Arrays::stream)
                    .map(PathPatternRequestMatcher.withDefaults()::matcher)
                    .toList();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PATHS.stream()
                .anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String accessToken = JwtUtil.resolveToken(request);
            // 토큰이 없는 경우 인증 객체를 설정하지 않고 넘김
            // SecurityConfig의 .anyRequest().authenticated()에서 인가 처리됨
            if (accessToken != null) {
                Claims claims = jwtService.validateAccessTokenAndGetClaims(accessToken);
                if (jwtService.isAccessTokenBlacklisted(accessToken)) {
                    throw new GeneralException(ErrorStatus.JWT_BLACKLISTED);
                }
                Long userId = jwtService.getUserIdFromClaims(claims);
                MDC.put("userId", String.valueOf(userId));

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                String tokenType = claims.get("tokenType", String.class);
                if ("ADMIN".equals(tokenType)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else {
                    // 탈퇴(soft-delete)했거나 존재하지 않는 유저는 인증 실패 처리
                    User user = userRepository.findByIdAndDeletedFalse(userId)
                            .orElseThrow(() -> new GeneralException(ErrorStatus.JWT_USER_WITHDRAWN));
                    if (user.isSuspended()) {
                        throw new GeneralException(ErrorStatus.USER_SUSPENDED);
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            log.warn("[*] JwtFilter GeneralException : {}", e.getMessage());
            sendErrorResponse(response, e);
        } finally {
            MDC.remove("userId");
        }
    }

    // GeneralException을 ApiResponse 포맷으로 직접 response에 작성
    private void sendErrorResponse(HttpServletResponse response, GeneralException e) throws IOException {
        response.setStatus(e.getErrorStatus().getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = new ApiResponse<>(
                false,
                e.getErrorStatus().getCode(),
                e.getErrorStatus().getMessage(),
                null
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

}
