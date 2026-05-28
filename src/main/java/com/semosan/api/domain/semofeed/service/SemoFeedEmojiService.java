package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.entity.SemoFeedEmoji;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.notification.service.SemoFeedNotificationService;
import com.semosan.api.domain.semofeed.repository.SemoFeedEmojiRepository;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemoFeedEmojiService {

    private final SemoFeedEmojiRepository semoFeedEmojiRepository;
    private final SemoFeedRepository semoFeedRepository;
    private final SemoFeedNotificationService semoFeedNotificationService;
    private final UserReader userReader;

    @Transactional
    // 이모지 반응을 토글하고 변경 후 해당 타입의 개수를 반환합니다.
    public SemoFeedEmojiToggleResponse toggleWithCount(
            Long userId,
            Long semoFeedId,
            SemoFeedEmojiType emojiType
    ) {
        SemoFeed semoFeed = findSemoFeedOrThrow(semoFeedId);
        User user = userReader.findActiveUserById(userId);

        boolean reacted = toggle(semoFeed, user, emojiType);
        if (reacted) {
            // 새 반응 등록일 때만 작성자 알림을 발송합니다.
            semoFeedNotificationService.sendEmojiNotification(semoFeed, user, emojiType);
        }
        long count = semoFeedEmojiRepository.countBySemoFeedAndEmojiType(semoFeed, emojiType);

        return new SemoFeedEmojiToggleResponse(emojiType, reacted, count);
    }

    // 기존 반응이 있으면 취소하고, 없으면 새 반응을 저장합니다.
    private boolean toggle(SemoFeed semoFeed, User user, SemoFeedEmojiType emojiType) {
        Optional<SemoFeedEmoji> existing = semoFeedEmojiRepository.findBySemoFeedAndUserAndEmojiType(
                semoFeed,
                user,
                emojiType
        );

        if (existing.isPresent()) {
            semoFeedEmojiRepository.delete(existing.get());
            return false;
        }

        semoFeedEmojiRepository.save(SemoFeedEmoji.create(semoFeed, user, emojiType));
        return true;
    }

    // 세모피드가 없으면 도메인 예외를 던집니다.
    private SemoFeed findSemoFeedOrThrow(Long semoFeedId) {
        return semoFeedRepository.findById(semoFeedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SEMOFEED_NOT_FOUND));
    }

}
