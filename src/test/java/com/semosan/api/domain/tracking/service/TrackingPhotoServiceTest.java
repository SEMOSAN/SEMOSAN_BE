package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.dto.request.TrackingPhotoUploadRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingPhotoResponse;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingPhotoRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingPhotoServiceTest {

    @Mock
    private TrackingPhotoRepository trackingPhotoRepository;

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @InjectMocks
    private TrackingPhotoService trackingPhotoService;

    @Test
    void uploadSavesPhotoWhenSessionIsActiveAndMilestoneIsNew() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.IN_PROGRESS);
        TrackingPhotoUploadRequest request = uploadRequest(0);
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(trackingPhotoRepository.existsByTrackingSession_IdAndMilestoneIndex(10L, 0)).thenReturn(false);
        when(trackingPhotoRepository.save(any(TrackingPhoto.class))).thenAnswer(invocation -> {
            TrackingPhoto photo = invocation.getArgument(0);
            ReflectionTestUtils.setField(photo, "id", 100L);
            return photo;
        });

        TrackingPhotoResponse response = trackingPhotoService.upload(1L, 10L, request);

        assertThat(response.photoId()).isEqualTo(100L);
        assertThat(response.trackingSessionId()).isEqualTo(10L);
        assertThat(response.milestoneIndex()).isZero();
        assertThat(response.imageUrl()).isEqualTo("image.jpg");
        ArgumentCaptor<TrackingPhoto> captor = ArgumentCaptor.forClass(TrackingPhoto.class);
        verify(trackingPhotoRepository).save(captor.capture());
        assertThat(captor.getValue().getTrackingSession()).isSameAs(session);
    }

    @Test
    void uploadAllowsPausedSession() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.PAUSED);
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(trackingPhotoRepository.existsByTrackingSession_IdAndMilestoneIndex(10L, 1)).thenReturn(false);
        when(trackingPhotoRepository.save(any(TrackingPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackingPhotoResponse response = trackingPhotoService.upload(1L, 10L, uploadRequest(1));

        assertThat(response.milestoneIndex()).isEqualTo(1);
    }

    @Test
    void uploadThrowsWhenSessionInactive() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.COMPLETED);
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> trackingPhotoService.upload(1L, 10L, uploadRequest(0)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_PHOTO_SESSION_INACTIVE);
    }

    @Test
    void uploadThrowsWhenPhotoAlreadyExistsForMilestone() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.IN_PROGRESS);
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(trackingPhotoRepository.existsByTrackingSession_IdAndMilestoneIndex(10L, 0)).thenReturn(true);

        assertThatThrownBy(() -> trackingPhotoService.upload(1L, 10L, uploadRequest(0)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_PHOTO_DUPLICATE);
    }

    @Test
    void listBySessionReturnsPhotosOrderedByRepositoryResult() {
        TrackingSession session = session(10L, 1L, TrackingSessionStatus.IN_PROGRESS);
        TrackingPhoto first = photo(session, 0);
        TrackingPhoto second = photo(session, 1);
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(trackingPhotoRepository.findByTrackingSession_IdOrderByMilestoneIndexAsc(10L))
                .thenReturn(List.of(first, second));

        List<TrackingPhotoResponse> responses = trackingPhotoService.listBySession(1L, 10L);

        assertThat(responses).extracting(TrackingPhotoResponse::milestoneIndex).containsExactly(0, 1);
    }

    @Test
    void listBySessionThrowsWhenSessionOwnedByOtherUser() {
        when(trackingSessionRepository.findById(10L))
                .thenReturn(Optional.of(session(10L, 2L, TrackingSessionStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> trackingPhotoService.listBySession(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    @Test
    void listBySessionThrowsWhenSessionMissing() {
        when(trackingSessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingPhotoService.listBySession(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_NOT_FOUND);
    }

    private TrackingPhotoUploadRequest uploadRequest(Integer milestoneIndex) {
        return new TrackingPhotoUploadRequest(milestoneIndex, 500.0, "image.jpg",
                LocalDateTime.of(2026, 8, 6, 13, 0), 37.5, 127.0, 123.4);
    }

    private TrackingPhoto photo(TrackingSession session, Integer milestoneIndex) {
        TrackingPhoto photo = TrackingPhoto.create(session, milestoneIndex, 500.0,
                "image-" + milestoneIndex + ".jpg", LocalDateTime.now(), 37.5, 127.0, null);
        ReflectionTestUtils.setField(photo, "id", milestoneIndex.longValue());
        return photo;
    }

    private TrackingSession session(Long id, Long userId, TrackingSessionStatus status) {
        TrackingSession session = TrackingSession.create(user(userId), mountain(), null, true);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "status", status);
        return session;
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Mountain mountain() {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", 1L);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
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
