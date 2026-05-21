package com.semosan.api.common.alert;

import com.semosan.api.common.alert.dto.DiscordEmbed;
import com.semosan.api.common.alert.dto.DiscordMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerErrorAlertService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH시 mm분 ss초");
    private static final int ERROR_COLOR = 0xED4245;
    private static final int MAX_STACK_TRACE_LENGTH = 1000;
    private static final int MAX_EMBED_DESCRIPTION_LENGTH = 4000;

    private final DiscordAlertClient discordAlertClient;
    private final Environment environment;

    @Async("discordAlertExecutor")
    public void notify(int status, Exception exception, HttpServletRequest request) {
        discordAlertClient.send(buildMessage(status, exception, request));
    }

    private DiscordMessage buildMessage(int status, Exception exception, HttpServletRequest request) {
        String description = """
                ### 에러 발생 시간
                %s
                ### 실행 프로필
                %s
                ### 요청 엔드포인트
                %s
                ### 응답 상태
                %d
                ### 요청 클라이언트
                %s
                ### 에러 메시지
                %s
                ### 에러 스택 트레이스
                ```text
                %s
                ```
                """.formatted(
                ZonedDateTime.now(KOREA_ZONE).format(TIME_FORMATTER),
                activeProfiles(),
                endpoint(request),
                status,
                client(request),
                sanitize(exception.getMessage()),
                stackTrace(exception)
        );

        return new DiscordMessage(
                "# 🚨 서버 에러 발생 🚨",
                List.of(new DiscordEmbed("에러 정보", truncate(description, MAX_EMBED_DESCRIPTION_LENGTH), ERROR_COLOR))
        );
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return String.join(",", Arrays.asList(profiles));
    }

    private String endpoint(HttpServletRequest request) {
        return "[%s] %s".formatted(request.getMethod(), request.getRequestURL());
    }

    private String client(HttpServletRequest request) {
        String userIdentifier = request.getUserPrincipal() == null
                ? ""
                : " / [UserId]: " + sanitize(request.getUserPrincipal().getName());
        return "[IP]: %s%s / [User-Agent]: %s".formatted(
                clientIp(request),
                userIdentifier,
                sanitize(request.getHeader("User-Agent"))
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return truncate(maskSensitiveValues(writer.toString()), MAX_STACK_TRACE_LENGTH);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return maskSensitiveValues(value)
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String maskSensitiveValues(String value) {
        return value.replaceAll("(?i)(authorization|cookie|token|secret|password)=\\S+", "$1=***");
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
