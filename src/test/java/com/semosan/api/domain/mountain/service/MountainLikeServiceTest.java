package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainLikeToggleResponse;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.MountainLike;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MountainLikeServiceTest {

    @Mock
    private MountainLikeRepository mountainLikeRepository;

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MountainLikeService mountainLikeService;

    @Test
    void toggleMountainLikeCreatesLikeWhenNotLiked() {
        User user = user(1L);
        Mountain mountain = mountain(10L);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(mountainLikeRepository.findByUser_IdAndMountain_Id(1L, 10L)).thenReturn(Optional.empty());

        MountainLikeToggleResponse response = mountainLikeService.toggleMountainLike(1L, 10L);

        assertThat(response.liked()).isTrue();
        verify(mountainLikeRepository).save(any(MountainLike.class));
    }

    @Test
    void toggleMountainLikeThrowsWhenMountainMissing() {
        when(userReader.findActiveUserById(1L)).thenReturn(user(1L));
        when(mountainRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mountainLikeService.toggleMountainLike(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void toggleMountainLikeDeletesLikeWhenAlreadyLiked() {
        User user = user(1L);
        Mountain mountain = mountain(10L);
        MountainLike mountainLike = MountainLike.create(user, mountain);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(mountainLikeRepository.findByUser_IdAndMountain_Id(1L, 10L)).thenReturn(Optional.of(mountainLike));

        MountainLikeToggleResponse response = mountainLikeService.toggleMountainLike(1L, 10L);

        assertThat(response.liked()).isFalse();
        verify(mountainLikeRepository).delete(mountainLike);
    }

    // LikeConflictHandler 도입으로 동시 요청 충돌은 예외 대신 성공(이미 좋아요한 것으로 간주)으로 흡수한다.
    @Test
    void toggleMountainLikeReturnsLikedWhenConcurrentDuplicateDetected() {
        User user = user(1L);
        Mountain mountain = mountain(10L);

        when(userReader.findActiveUserById(1L)).thenReturn(user);
        when(mountainRepository.findById(10L)).thenReturn(Optional.of(mountain));
        when(mountainLikeRepository.findByUser_IdAndMountain_Id(1L, 10L)).thenReturn(Optional.empty());
        when(mountainLikeRepository.save(any(MountainLike.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        ReflectionTestUtils.setField(mountainLikeService, "entityManager", entityManager);

        MountainLikeToggleResponse response = mountainLikeService.toggleMountainLike(1L, 10L);

        assertThat(response.liked()).isTrue();
        verify(entityManager).clear();
    }

    @Test
    void getLikedMountainsMapsLikedMountainPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        MountainLike mountainLike = MountainLike.create(user(1L), mountain(10L));
        when(mountainLikeRepository.findAllByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(mountainLike), pageable, 1));

        Page<LikedMountainResponse> result = mountainLikeService.getLikedMountains(1L, pageable);

        verify(userReader).findActiveUserById(1L);
        assertThat(result.getContent().getFirst().mountainId()).isEqualTo(10L);
        assertThat(result.getContent().getFirst().name()).isEqualTo("관악산");
    }

    private User user(Long id) {
        User user = User.createTestUser("user-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Mountain mountain(Long id) {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        ReflectionTestUtils.setField(mountain, "address", "서울 관악구");
        ReflectionTestUtils.setField(mountain, "altitude", 632.2);
        ReflectionTestUtils.setField(mountain, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(mountain, "imageUrls", List.of("image.jpg"));
        return mountain;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
