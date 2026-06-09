package com.semosan.api.common.config;

import com.semosan.api.common.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * 스웨거 관련 경로
     */
    public static final String[] SWAGGER_URIS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui.html"
    };

    /**
     * 소셜 인증(카카오, 애플) 관련 경로
     */
    public static final String[] OAUTH_URIS = {
            "/api/oauth/kakao/login",
            "/api/oauth/apple/login",
    };

    /**
     * 인증(회원가입, 로그인 등) 관련 경로
     */
    public static final String[] AUTH_URIS = {
            "/api/auth/test/login",
            "/api/auth/token/reissue"
    };

    /**
     * WebSocket(STOMP) 엔드포인트.
     * 핸드셰이크는 HTTP JWT 필터로 인증하지 않고 통과시키고,
     * STOMP CONNECT 프레임에서 StompAuthChannelInterceptor 가 JWT 를 검증한다.
     */
    public static final String[] WEBSOCKET_URIS = {
            "/ws/tracking/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_URIS).permitAll()
                        .requestMatchers(OAUTH_URIS).permitAll()
                        .requestMatchers(AUTH_URIS).permitAll()
                        .requestMatchers(WEBSOCKET_URIS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app-version").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration restConfig = new CorsConfiguration();
        restConfig.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8081", "https://lgenius.site"));
        restConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        restConfig.setAllowedHeaders(List.of("*"));
        restConfig.setAllowCredentials(true);

        CorsConfiguration wsConfig = new CorsConfiguration();
        wsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "https://lgenius.site"));
        wsConfig.setAllowedMethods(List.of("GET"));
        wsConfig.setAllowedHeaders(List.of("*"));
        wsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/ws/**", wsConfig);
        source.registerCorsConfiguration("/**", restConfig);
        return source;
    }

}
