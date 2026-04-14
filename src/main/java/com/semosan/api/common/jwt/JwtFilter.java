package com.semosan.api.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String accessToken = resolveToken(request);
            if (accessToken != null) {
                jwtService.validateAccessToken(accessToken);
                Long userId = jwtService.getUserIdFromJwtToken(accessToken);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            // GeneralException을 그대로 ApiResponse 포맷으로 직렬화하여 응답
            // Spring Security가 가로채지 않도록 직접 response에 작성
            log.warn("[*] JwtFilter GeneralException : {}", e.getMessage());
            sendErrorResponse(response, e);
        }
    }

    // Authorization 헤더에서 JWT 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
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
