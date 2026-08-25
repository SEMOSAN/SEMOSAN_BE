package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.dto.RecordPostResponse;
import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.repository.PostRepository;
import com.semosan.api.domain.community.post.repository.RecordPostRepository;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordPostServiceTest {

    @Mock
    private RecordPostRepository recordPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private HikingRecordRepository hikingRecordRepository;

    @Mock
    private HikingMemberRepository hikingMemberRepository;

    @Mock
    private PostAccessPolicy postAccessPolicy;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private RecordPostService recordPostService;

    @Test
    void createReturnsResponse() throws Exception {
        User author = user(1L, "author");
        HikingRecord hikingRecord = hikingRecord(100L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));
        RecordPost savedPost = RecordPost.create(author, "본문", hikingRecord);
        ReflectionTestUtils.setField(savedPost, "id", 10L);

        when(userReader.findActiveUserById(1L)).thenReturn(author);
        when(hikingRecordRepository.findById(100L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, author)).thenReturn(true);
        when(recordPostRepository.save(any(RecordPost.class))).thenReturn(savedPost);

        RecordPostResponse result = recordPostService.create(1L, 100L, "본문");

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.content()).isEqualTo("본문");
        assertThat(result.hikingRecord().courseName()).isEqualTo("과천향교 출발 코스");
    }

    @Test
    void createThrowsWhenHikingRecordNotFound() {
        User author = user(1L, "author");

        when(userReader.findActiveUserById(1L)).thenReturn(author);
        when(hikingRecordRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordPostService.create(1L, 100L, "본문"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_NOT_FOUND);
        verify(hikingMemberRepository, never()).existsByHikingRecordAndUser(any(), any());
        verify(recordPostRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenAuthorIsNotHikingMember() throws Exception {
        User author = user(1L, "author");
        HikingRecord hikingRecord = hikingRecord(100L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스"));

        when(userReader.findActiveUserById(1L)).thenReturn(author);
        when(hikingRecordRepository.findById(100L)).thenReturn(Optional.of(hikingRecord));
        when(hikingMemberRepository.existsByHikingRecordAndUser(hikingRecord, author)).thenReturn(false);

        assertThatThrownBy(() -> recordPostService.create(1L, 100L, "본문"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.HIKING_RECORD_FORBIDDEN);
        verify(recordPostRepository, never()).save(any());
    }

    @Test
    void getListUsesViewerSpecificVisibleQuery() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));
        PageRequest pageable = PageRequest.of(0, 10);
        Page<RecordPost> page = new PageImpl<>(List.of(post), pageable, 1);

        when(recordPostRepository.findVisibleByViewerId(1L, pageable)).thenReturn(page);

        Page<RecordPostResponse> result = recordPostService.getList(1L, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        assertThat(result.getContent().get(0).hikingRecord().mountainName()).isEqualTo("관악산");
        verify(recordPostRepository).findVisibleByViewerId(eq(1L), eq(pageable));
        verify(recordPostRepository, never()).findAllByDeletedFalse(pageable);
    }

    @Test
    void getMyListUsesSummaryFetchQuery() throws Exception {
        User author = user(1L, "author");
        RecordPost post = recordPost(10L, author);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<RecordPost> page = new PageImpl<>(List.of(post), pageable, 1);

        when(userReader.findActiveUserById(1L)).thenReturn(author);
        when(recordPostRepository.findByAuthorAndDeletedFalseWithSummary(author, pageable)).thenReturn(page);

        Page<RecordPostResponse> result = recordPostService.getMyList(1L, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).author().id()).isEqualTo(1L);
        verify(recordPostRepository).findByAuthorAndDeletedFalseWithSummary(eq(author), eq(pageable));
    }

    @Test
    void getMyListPropagatesActiveUserLookupFailure() {
        GeneralException exception = new GeneralException(ErrorStatus.USER_NOT_FOUND);
        when(userReader.findActiveUserById(1L)).thenThrow(exception);

        assertThatThrownBy(() -> recordPostService.getMyList(1L, PageRequest.of(0, 10)))
                .isSameAs(exception);
        verify(recordPostRepository, never()).findByAuthorAndDeletedFalseWithSummary(any(), any());
    }

    @Test
    void getDetailValidatesBlockPolicyBeforeIncreasingViewCount() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));
        ReflectionTestUtils.setField(post, "viewCount", 4);

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        RecordPostResponse result = recordPostService.getDetail(1L, 10L);

        assertThat(result.viewCount()).isEqualTo(5);
        assertThat(post.getViewCount()).isEqualTo(4);
        verify(postAccessPolicy).validateReadable(1L, post);
        verify(postRepository).increaseViewCount(10L);
    }

    @Test
    void getDetailThrowsWhenViewerBlockedAuthor() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));
        org.mockito.Mockito.doThrow(new GeneralException(ErrorStatus.POST_AUTHOR_BLOCKED))
                .when(postAccessPolicy).validateReadable(1L, post);

        assertThatThrownBy(() -> recordPostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_AUTHOR_BLOCKED);
        assertThat(post.getViewCount()).isZero();
        verify(postRepository, never()).increaseViewCount(10L);
    }

    @Test
    void getDetailThrowsWhenPostDoesNotExist() {
        when(recordPostRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordPostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
        verify(postAccessPolicy, never()).validateReadable(eq(1L), any());
        verify(postRepository, never()).increaseViewCount(10L);
    }

    @Test
    void getDetailThrowsWhenPostIsDeleted() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));
        post.softDelete();

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> recordPostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_DELETED);
        verify(postAccessPolicy, never()).validateReadable(1L, post);
        verify(postRepository, never()).increaseViewCount(10L);
    }

    @Test
    void deleteSoftDeletesWhenRequesterOwnsPost() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        recordPostService.delete(10L, 2L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void deleteThrowsWhenRequesterDoesNotOwnPost() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> recordPostService.delete(10L, 3L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_FORBIDDEN);
        assertThat(post.isDeleted()).isFalse();
    }

    @Test
    void deleteThrowsWhenPostDoesNotExist() {
        when(recordPostRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordPostService.delete(10L, 2L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    @Test
    void deleteThrowsWhenPostIsDeleted() throws Exception {
        RecordPost post = recordPost(10L, user(2L, "author"));
        post.softDelete();

        when(recordPostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> recordPostService.delete(10L, 2L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_DELETED);
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private RecordPost recordPost(Long id, User author) throws Exception {
        RecordPost post = RecordPost.create(author, "본문", hikingRecord(100L, course(mountain(20L, "관악산"), 30L, "과천향교 출발 코스")));
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Mountain mountain(Long id, String name) throws Exception {
        Constructor<Mountain> constructor = Mountain.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Mountain mountain = constructor.newInstance();
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", name);
        return mountain;
    }

    private Course course(Mountain mountain, Long id, String name) throws Exception {
        Constructor<Course> constructor = Course.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Course course = constructor.newInstance();
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "mountain", mountain);
        ReflectionTestUtils.setField(course, "name", name);
        return course;
    }

    private HikingRecord hikingRecord(Long id, Course course) throws Exception {
        Constructor<HikingRecord> constructor = HikingRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        HikingRecord hikingRecord = constructor.newInstance();
        ReflectionTestUtils.setField(hikingRecord, "id", id);
        ReflectionTestUtils.setField(hikingRecord, "mountain", course.getMountain());
        ReflectionTestUtils.setField(hikingRecord, "course", course);
        ReflectionTestUtils.setField(hikingRecord, "duration", 3600);
        ReflectionTestUtils.setField(hikingRecord, "maxAltitude", 629.0);
        ReflectionTestUtils.setField(hikingRecord, "calories", 500);
        return hikingRecord;
    }
}
