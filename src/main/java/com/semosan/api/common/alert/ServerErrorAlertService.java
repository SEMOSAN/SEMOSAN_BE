package com.semosan.api.common.alert;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ServerErrorAlertService {

    private static final int MAX_MESSAGE_LENGTH = 300;
    private static final int MAX_USER_AGENT_LENGTH = 180;
    private static final int MAX_CONTENT_LENGTH = 1900;

    private final DiscordAlertClient discordAlertClient;
    private final Environment environment;

    @Async("notificationTaskExecutor")
    public void notify(int status, Exception exception, HttpServletRequest request) {
        discordAlertClient.send(buildMessage(status, exception, request));
    }

    private String buildMessage(int status, Exception exception, HttpServletRequest request) {
        String content = """
                [SEMOSAN API %d ERROR]

                Profile: %s
                Method: %s
                URI: %s
                Status: %d
                Exception: %s
                Message: %s
                Client IP: %s
                User-Agent: %s
                Time: %s
                """.formatted(
                status,
                activeProfiles(),
                request.getMethod(),
                request.getRequestURI(),
                status,
                exception.getClass().getSimpleName(),
                sanitize(exception.getMessage(), MAX_MESSAGE_LENGTH),
                clientIp(request),
                sanitize(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH),
                OffsetDateTime.now()
        );

        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH) + "\n...";
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return String.join(",", Arrays.asList(profiles));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitize(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String sanitized = value
                .replaceAll("(?i)(authorization|cookie|token|secret|password)=\\S+", "$1=***")
                .replace("\n", " ")
                .replace("\r", " ");
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength) + "...";
    }
}
