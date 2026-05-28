package com.semosan.api.domain.semofeed.notification.service;

import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemoFeedNotificationService {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 세모피드에 새 이모지 반응이 등록되면 작성자에게 알림을 보냅니다.
    public void sendEmojiNotification(SemoFeed semoFeed, User reactor, SemoFeedEmojiType emojiType) {
        User receiver = semoFeed.getUser();
        sendIfEligible(
                receiver,
                reactor,
                Map.of(
                        "actorId", reactor.getId(),
                        "actorName", reactor.displayName(),
                        "semoFeedId", semoFeed.getId(),
                        "emojiType", emojiType.name()
                )
        );
    }

    // 본인 반응과 탈퇴한 작성자는 알림 발송 대상에서 제외합니다.
    private void sendIfEligible(User receiver, User reactor, Map<String, Object> params) {
        if (receiver.getId().equals(reactor.getId())) {
            return;
        }
        if (!userRepository.existsByIdAndDeletedFalse(receiver.getId())) {
            return;
        }

        notificationService.send(receiver.getId(), NotificationType.SEMOFEED_EMOJI, params);
    }
}
