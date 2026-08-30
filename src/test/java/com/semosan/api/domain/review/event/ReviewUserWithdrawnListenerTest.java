package com.semosan.api.domain.review.event;

import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReviewUserWithdrawnListenerTest {

    @Test
    void onUserWithdrawnDeletesReviews() {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewUserWithdrawnListener listener = new ReviewUserWithdrawnListener(reviewRepository);

        listener.onUserWithdrawn(new UserWithdrawnEvent(1L));

        verify(reviewRepository).deleteByUser_Id(1L);
    }
}
