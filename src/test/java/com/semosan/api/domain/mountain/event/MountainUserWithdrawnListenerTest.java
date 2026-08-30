package com.semosan.api.domain.mountain.event;

import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MountainUserWithdrawnListenerTest {

    @Test
    void onUserWithdrawnDeletesMountainAndCourseLikes() {
        MountainLikeRepository mountainLikeRepository = mock(MountainLikeRepository.class);
        CourseLikeRepository courseLikeRepository = mock(CourseLikeRepository.class);
        MountainUserWithdrawnListener listener =
                new MountainUserWithdrawnListener(mountainLikeRepository, courseLikeRepository);

        listener.onUserWithdrawn(new UserWithdrawnEvent(1L));

        verify(mountainLikeRepository).deleteByUser_Id(1L);
        verify(courseLikeRepository).deleteByUser_Id(1L);
    }
}
