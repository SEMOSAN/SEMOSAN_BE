package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.entity.SemoFeedEmoji;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.repository.SemoFeedEmojiRepository;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemoFeedEmojiService {

    private final SemoFeedEmojiRepository semoFeedEmojiRepository;
    private final SemoFeedRepository semoFeedRepository;
    private final UserRepository userRepository;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public SemoFeedEmojiToggleResponse toggleWithCount(
            Long userId,
            Long semoFeedId,
            SemoFeedEmojiType emojiType
    ) {
        SemoFeed semoFeed = findSemoFeedOrThrow(semoFeedId);
        User user = findUserOrThrow(userId);
        validateNotOwner(semoFeed, userId);

        boolean reacted = toggle(semoFeed, user, emojiType);
        long count = semoFeedEmojiRepository.countBySemoFeedAndEmojiType(semoFeed, emojiType);

        return new SemoFeedEmojiToggleResponse(emojiType, reacted, count);
    }

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

        try {
            semoFeedEmojiRepository.save(SemoFeedEmoji.create(semoFeed, user, emojiType));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "SemoFeedEmoji 동시 요청 감지: semoFeedId={}, userId={}, emojiType={}",
                    semoFeed.getId(),
                    user.getId(),
                    emojiType
            );
            return true;
        }
    }

    private SemoFeed findSemoFeedOrThrow(Long semoFeedId) {
        return semoFeedRepository.findById(semoFeedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SEMOFEED_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private void validateNotOwner(SemoFeed semoFeed, Long userId) {
        if (semoFeed.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.SEMOFEED_EMOJI_SELF_NOT_ALLOWED);
        }
    }
}
