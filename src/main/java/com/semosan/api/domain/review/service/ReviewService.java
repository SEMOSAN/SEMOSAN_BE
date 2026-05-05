package com.semosan.api.domain.review.service;

import com.semosan.api.domain.review.entity.Review;
import com.semosan.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int DEFAULT_REVIEW_LIMIT = 20;

    private final ReviewRepository reviewRepository;

    public List<Review> getReviewsByMountainId(Long mountainId) {
        return reviewRepository.findRecentByMountainId(mountainId, DEFAULT_REVIEW_LIMIT);
    }
}
