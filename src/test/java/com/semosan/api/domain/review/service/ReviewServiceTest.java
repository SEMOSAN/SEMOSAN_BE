package com.semosan.api.domain.review.service;

import com.semosan.api.domain.review.entity.Review;
import com.semosan.api.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void getReviewsByMountainIdReturnsRecentReviewsWithDefaultLimit() {
        Review review = mock(Review.class);
        when(reviewRepository.findRecentByMountainId(1L, 20)).thenReturn(List.of(review));

        List<Review> result = reviewService.getReviewsByMountainId(1L);

        assertThat(result).containsExactly(review);
        verify(reviewRepository).findRecentByMountainId(1L, 20);
    }
}
