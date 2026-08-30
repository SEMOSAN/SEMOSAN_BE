package com.semosan.api.domain.user.event;

import com.semosan.api.common.alert.DiscordAlertClient;
import com.semosan.api.common.alert.dto.DiscordEmbed;
import com.semosan.api.common.alert.dto.DiscordMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int SIGNUP_COLOR = 0x57F287;

    private final DiscordAlertClient discordAlertClient;

    @Async("discordAlertExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            discordAlertClient.sendSignup(buildMessage(event));
        } catch (Exception e) {
            log.warn("신규 가입 Discord 알림 생성 실패: userId={}, msg={}", event.userId(), e.getMessage());
        }
    }

    private DiscordMessage buildMessage(UserRegisteredEvent event) {
        String description = """
                ### 유저 ID
                %d
                ### 닉네임
                %s
                ### 가입 경로
                %s / %s
                ### 가입 시각
                %s""".formatted(
                event.userId(),
                event.nickname(),
                event.provider(),
                event.deviceType(),
                registeredAt(event)
        );

        return new DiscordMessage(
                "# 🎉 신규 가입",
                List.of(new DiscordEmbed("가입 정보", description, SIGNUP_COLOR))
        );
    }

    private String registeredAt(UserRegisteredEvent event) {
        if (event.registeredAt() == null) {
            return "-";
        }
        return event.registeredAt().format(TIME_FORMATTER);
    }
}
