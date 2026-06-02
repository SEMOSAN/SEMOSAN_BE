package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.like.repository.PostLikeRepository;
import com.semosan.api.domain.community.post.dto.FreePostDetailResponse;
import com.semosan.api.domain.community.post.dto.FreePostListResponse;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.PostImage;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.community.post.repository.PostImageRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreePostServiceTest {

    @Mock
    private FreePostRepository freePostRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private FreePostService freePostService;

    @Test
    void getListUsesViewerSpecificVisibleQuery() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        Page<FreePost> page = new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1);

        when(freePostRepository.findVisibleByViewerId(1L, PageRequest.of(0, 10))).thenReturn(page);
        when(postLikeRepository.countByPostIdsGrouped(anyList())).thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));
        when(commentRepository.countByPostIdsGrouped(anyList())).thenReturn(List.<Object[]>of(new Object[]{10L, 2L}));
        when(postImageRepository.findMainImagesByPostIds(anyList())).thenReturn(List.of());
        when(postImageRepository.countByPostIdsGrouped(anyList())).thenReturn(List.of());

        Page<FreePostListResponse> result = freePostService.getList(1L, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        verify(freePostRepository).findVisibleByViewerId(eq(1L), eq(PageRequest.of(0, 10)));
    }

    @Test
    void getDetailReturnsLikedByMe() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(false);
        when(postImageRepository.findByPostOrderBySortOrderAsc(post)).thenReturn(List.of());
        when(postLikeRepository.countByPost(post)).thenReturn(3L);
        when(commentRepository.countByPostAndDeletedFalse(post)).thenReturn(2L);
        when(postLikeRepository.existsByPostIdAndUserId(10L, 1L)).thenReturn(true);

        FreePostDetailResponse result = freePostService.getDetail(1L, 10L);

        assertThat(result.likedByMe()).isTrue();
        assertThat(result.likeCount()).isEqualTo(3L);
        assertThat(result.commentCount()).isEqualTo(2L);
    }

    @Test
    void getDetailThrowsWhenViewerBlockedAuthor() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> freePostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_AUTHOR_BLOCKED);
        verify(postImageRepository, never()).findByPostOrderBySortOrderAsc(post);
    }

    @Test
    void deleteSoftDeletesWhenRequesterOwnsPost() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        freePostService.delete(10L, 2L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void deleteThrowsWhenRequesterDoesNotOwnPost() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> freePostService.delete(10L, 3L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_FORBIDDEN);
        assertThat(post.isDeleted()).isFalse();
    }

    private User user(Long id, String oauthId) {
        User user = User.createTestUser(oauthId, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", oauthId);
        return user;
    }

    private FreePost freePost(Long id, User author, String title, String content) throws Exception {
        Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
        constructor.setAccessible(true);
        FreePost post = constructor.newInstance(author, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
