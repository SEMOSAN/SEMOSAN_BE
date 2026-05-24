package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemoFeedService {

    private final SemoFeedRepository semoFeedRepository;
    private final UserRepository userRepository;

    @Transactional
    public SemoFeedResponse create(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        SemoFeed semoFeed = SemoFeed.create(user, imageUrl);
        return SemoFeedResponse.from(semoFeedRepository.save(semoFeed));
    }

    public Page<SemoFeedResponse> listPublic(Pageable pageable) {
        return semoFeedRepository.findPublic(pageable)
                .map(SemoFeedResponse::from);
    }

    public List<SemoFeedResponse> listMine(Long userId) {
        return semoFeedRepository.findByUserId(userId).stream()
                .map(SemoFeedResponse::from)
                .toList();
    }

    @Transactional
    public boolean togglePublic(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = findOwned(userId, semoFeedId);
        semoFeed.togglePublic();
        return semoFeed.isPublic();
    }

    @Transactional
    public void delete(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = findOwned(userId, semoFeedId);
        semoFeedRepository.delete(semoFeed);
    }

    private SemoFeed findOwned(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = semoFeedRepository.findById(semoFeedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SEMOFEED_NOT_FOUND));
        if (!semoFeed.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.SEMOFEED_FORBIDDEN);
        }
        return semoFeed;
    }
}
