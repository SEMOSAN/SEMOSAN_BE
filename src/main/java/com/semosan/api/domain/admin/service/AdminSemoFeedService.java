package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.response.AdminSemoFeedResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSemoFeedService {

    private final SemoFeedRepository semoFeedRepository;

    @Transactional(readOnly = true)
    public Page<AdminSemoFeedResponse> getFeeds(Pageable pageable) {
        return semoFeedRepository.findAllForAdmin(pageable)
                .map(AdminSemoFeedResponse::from);
    }

    @Transactional
    public void updateVisibility(Long semoFeedId, boolean isPublic) {
        SemoFeed feed = findById(semoFeedId);
        feed.updatePublic(isPublic);
    }

    @Transactional
    public void delete(Long semoFeedId) {
        SemoFeed feed = findById(semoFeedId);
        semoFeedRepository.delete(feed);
    }

    private SemoFeed findById(Long semoFeedId) {
        return semoFeedRepository.findById(semoFeedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SEMOFEED_NOT_FOUND));
    }
}
