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
import com.semosan.api.domain.community.post.repository.PostRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreePostServiceTest {

    @Mock
    private FreePostRepository freePostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostAccessPolicy postAccessPolicy;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private FreePostService freePostService;

    @Test
    void createSavesPostAndImagesWithDefaultMainImageIndex() {
        User author = user(2L, "author");

        when(userReader.findActiveUserById(2L)).thenReturn(author);
        when(freePostRepository.save(org.mockito.ArgumentMatchers.any(FreePost.class)))
                .thenAnswer(invocation -> {
                    FreePost post = invocation.getArgument(0);
                    ReflectionTestUtils.setField(post, "id", 10L);
                    return post;
                });
        when(postImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        FreePostDetailResponse result = freePostService.create(
                2L,
                "제목",
                "본문",
                List.of("https://example.com/1.png", "https://example.com/2.png"),
                null
        );

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.content()).isEqualTo("본문");
        assertThat(result.images()).hasSize(2);
        assertThat(result.images().get(0).main()).isTrue();
        assertThat(result.images().get(1).main()).isFalse();
        assertThat(result.likeCount()).isZero();
        assertThat(result.commentCount()).isZero();
        assertThat(result.likedByMe()).isFalse();
    }

    @Test
    void createReturnsEmptyImagesWhenImageUrlsAreNullOrEmpty() {
        User author = user(2L, "author");

        when(userReader.findActiveUserById(2L)).thenReturn(author);
        when(freePostRepository.save(org.mockito.ArgumentMatchers.any(FreePost.class)))
                .thenAnswer(invocation -> {
                    FreePost post = invocation.getArgument(0);
                    ReflectionTestUtils.setField(post, "id", 10L);
                    return post;
                });

        FreePostDetailResponse nullImagesResult = freePostService.create(2L, "제목", "본문", null, null);
        FreePostDetailResponse emptyImagesResult = freePostService.create(2L, "제목", "본문", List.of(), null);

        assertThat(nullImagesResult.images()).isEmpty();
        assertThat(emptyImagesResult.images()).isEmpty();
        verify(postImageRepository, never()).saveAll(anyList());
    }

    @Test
    void createThrowsWhenContentIsBlank() {
        assertThatThrownBy(() -> freePostService.create(2L, "제목", " ", List.of(), null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_CONTENT_REQUIRED);
        verifyNoInteractions(userReader, freePostRepository, postImageRepository);
    }

    @Test
    void createThrowsWhenContentIsNull() {
        assertThatThrownBy(() -> freePostService.create(2L, "제목", null, List.of(), null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_CONTENT_REQUIRED);
        verifyNoInteractions(userReader, freePostRepository, postImageRepository);
    }

    @Test
    void createThrowsWhenMainImageIndexIsOutOfRange() {
        User author = user(2L, "author");

        when(userReader.findActiveUserById(2L)).thenReturn(author);

        assertThatThrownBy(() -> freePostService.create(
                2L,
                "제목",
                "본문",
                List.of("https://example.com/1.png"),
                1
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_IMAGE_INDEX_INVALID);
        verify(freePostRepository).save(org.mockito.ArgumentMatchers.any(FreePost.class));
        verify(postImageRepository, never()).saveAll(anyList());
    }

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
    void getListEnrichesThumbnailExtraImageCountAndDefaultCounts() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        PostImage mainImage = PostImage.create(post, "https://example.com/main.png", 0, true);
        PageRequest pageable = PageRequest.of(0, 10);

        when(freePostRepository.findVisibleByViewerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(postLikeRepository.countByPostIdsGrouped(List.of(10L))).thenReturn(List.of());
        when(commentRepository.countByPostIdsGrouped(List.of(10L))).thenReturn(List.of());
        when(postImageRepository.findMainImagesByPostIds(List.of(10L))).thenReturn(List.of(mainImage));
        when(postImageRepository.countByPostIdsGrouped(List.of(10L)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        Page<FreePostListResponse> result = freePostService.getList(1L, pageable);

        FreePostListResponse response = result.getContent().getFirst();
        assertThat(response.thumbnailUrl()).isEqualTo("https://example.com/main.png");
        assertThat(response.extraImageCount()).isEqualTo(2);
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
    }

    @Test
    void getListReturnsEmptyPageWithoutCountQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(freePostRepository.findVisibleByViewerId(1L, pageable)).thenReturn(Page.empty(pageable));

        Page<FreePostListResponse> result = freePostService.getList(1L, pageable);

        assertThat(result).isEmpty();
        verify(postLikeRepository, never()).countByPostIdsGrouped(anyList());
        verify(commentRepository, never()).countByPostIdsGrouped(anyList());
        verify(postImageRepository, never()).findMainImagesByPostIds(anyList());
        verify(postImageRepository, never()).countByPostIdsGrouped(anyList());
    }

    @Test
    void searchReturnsEmptyPageWhenKeywordIsBlank() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<FreePostListResponse> result = freePostService.search(1L, "  ", pageable);

        assertThat(result).isEmpty();
        verify(freePostRepository, never()).searchByKeyword(eq(1L), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @Test
    void searchReturnsEmptyPageWhenKeywordIsNull() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<FreePostListResponse> result = freePostService.search(1L, null, pageable);

        assertThat(result).isEmpty();
        verify(freePostRepository, never()).searchByKeyword(eq(1L), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @Test
    void searchStripsKeywordAndDropsSortBeforeQuerying() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        PageRequest sorted = PageRequest.of(1, 5, org.springframework.data.domain.Sort.by("createdAt").descending());
        PageRequest unsorted = PageRequest.of(1, 5);
        Page<FreePost> page = new PageImpl<>(List.of(post), unsorted, 1);

        when(freePostRepository.searchByKeyword(1L, "검색어", unsorted)).thenReturn(page);
        when(postLikeRepository.countByPostIdsGrouped(anyList())).thenReturn(List.of());
        when(commentRepository.countByPostIdsGrouped(anyList())).thenReturn(List.of());
        when(postImageRepository.findMainImagesByPostIds(anyList())).thenReturn(List.of());
        when(postImageRepository.countByPostIdsGrouped(anyList())).thenReturn(List.of());

        Page<FreePostListResponse> result = freePostService.search(1L, "  검색어  ", sorted);

        assertThat(result).hasSize(1);
        verify(freePostRepository).searchByKeyword(1L, "검색어", unsorted);
    }

    @Test
    void getDetailReturnsLikedByMe() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        ReflectionTestUtils.setField(post, "viewCount", 4);

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postImageRepository.findByPostOrderBySortOrderAsc(post)).thenReturn(List.of());
        when(postLikeRepository.countByPost(post)).thenReturn(3L);
        when(commentRepository.countByPostAndDeletedFalse(post)).thenReturn(2L);
        when(postLikeRepository.existsByPostIdAndUserId(10L, 1L)).thenReturn(true);

        FreePostDetailResponse result = freePostService.getDetail(1L, 10L);

        assertThat(result.likedByMe()).isTrue();
        assertThat(result.likeCount()).isEqualTo(3L);
        assertThat(result.commentCount()).isEqualTo(2L);
        assertThat(result.viewCount()).isEqualTo(5);
        assertThat(post.getViewCount()).isEqualTo(4);
        verify(postAccessPolicy).validateReadable(1L, post);
        verify(postRepository).increaseViewCount(10L);
    }

    @Test
    void getMyListFindsAuthorAndEnrichesPosts() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        PageRequest pageable = PageRequest.of(0, 10);

        when(userReader.findActiveUserById(2L)).thenReturn(author);
        when(freePostRepository.findByAuthorAndDeletedFalse(author, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(postLikeRepository.countByPostIdsGrouped(List.of(10L)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 4L}));
        when(commentRepository.countByPostIdsGrouped(List.of(10L)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));
        when(postImageRepository.findMainImagesByPostIds(List.of(10L))).thenReturn(List.of());
        when(postImageRepository.countByPostIdsGrouped(List.of(10L))).thenReturn(List.of());

        Page<FreePostListResponse> result = freePostService.getMyList(2L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().likeCount()).isEqualTo(4L);
        assertThat(result.getContent().getFirst().commentCount()).isEqualTo(5L);
        verify(freePostRepository).findByAuthorAndDeletedFalse(author, pageable);
    }

    @Test
    void updateChangesTitleContentAndImagesWhenRequesterOwnsPost() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "기존 제목", "기존 본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(postLikeRepository.countByPost(post)).thenReturn(3L);
        when(commentRepository.countByPostAndDeletedFalse(post)).thenReturn(2L);
        when(postLikeRepository.existsByPostIdAndUserId(10L, 2L)).thenReturn(true);

        FreePostDetailResponse result = updatePost(
                2L,
                List.of("https://example.com/1.png", "https://example.com/2.png"),
                1
        );

        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 본문");
        assertThat(result.title()).isEqualTo("수정 제목");
        assertThat(result.content()).isEqualTo("수정 본문");
        assertThat(result.likeCount()).isEqualTo(3L);
        assertThat(result.commentCount()).isEqualTo(2L);
        assertThat(result.likedByMe()).isTrue();
        assertThat(result.images()).hasSize(2);
        assertThat(result.images().get(0).imageUrl()).isEqualTo("https://example.com/1.png");
        assertThat(result.images().get(0).main()).isFalse();
        assertThat(result.images().get(1).imageUrl()).isEqualTo("https://example.com/2.png");
        assertThat(result.images().get(1).main()).isTrue();
        verify(postImageRepository).deleteByPost(post);
    }

    @Test
    void updateThrowsWhenRequesterDoesNotOwnPost() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> updatePost(3L, List.of(), null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_FORBIDDEN);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("본문");
        verify(postImageRepository, never()).deleteByPost(post);
    }

    @Test
    void updateThrowsWhenPostDoesNotExist() {
        when(freePostRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updatePost(2L, List.of(), null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    @Test
    void updateThrowsWhenPostIsDeleted() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        post.softDelete();

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> updatePost(2L, List.of(), null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_DELETED);
    }

    @Test
    void updateThrowsWhenMainImageIndexIsNegative() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> updatePost(2L, List.of("https://example.com/1.png"), -1))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_IMAGE_INDEX_INVALID);
        verify(postImageRepository).deleteByPost(post);
        verify(postImageRepository, never()).saveAll(anyList());
    }

    @Test
    void updateReplacesImagesAfterDeletingExistingImages() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(postLikeRepository.countByPost(post)).thenReturn(0L);
        when(commentRepository.countByPostAndDeletedFalse(post)).thenReturn(0L);
        when(postLikeRepository.existsByPostIdAndUserId(10L, 2L)).thenReturn(false);

        FreePostDetailResponse result = updatePost(2L, List.of("https://example.com/new.png"), 0);

        verify(postImageRepository).deleteByPost(post);
        verify(postImageRepository).saveAll(anyList());
        assertThat(result.images()).singleElement()
                .satisfies(image -> {
                    assertThat(image.imageUrl()).isEqualTo("https://example.com/new.png");
                    assertThat(image.sortOrder()).isZero();
                    assertThat(image.main()).isTrue();
                });
    }

    @Test
    void getDetailThrowsWhenViewerBlockedAuthor() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));
        org.mockito.Mockito.doThrow(new GeneralException(ErrorStatus.POST_AUTHOR_BLOCKED))
                .when(postAccessPolicy).validateReadable(1L, post);

        assertThatThrownBy(() -> freePostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_AUTHOR_BLOCKED);
        verify(postRepository, never()).increaseViewCount(10L);
        verify(postImageRepository, never()).findByPostOrderBySortOrderAsc(post);
    }

    @Test
    void getDetailThrowsWhenPostDoesNotExist() {
        when(freePostRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> freePostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
        verify(postAccessPolicy, never()).validateReadable(eq(1L), org.mockito.ArgumentMatchers.any());
        verify(postRepository, never()).increaseViewCount(10L);
    }

    @Test
    void getDetailThrowsWhenPostIsDeleted() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        post.softDelete();

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> freePostService.getDetail(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_DELETED);
        verify(postAccessPolicy, never()).validateReadable(1L, post);
        verify(postRepository, never()).increaseViewCount(10L);
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

    @Test
    void deleteThrowsWhenPostDoesNotExist() {
        when(freePostRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> freePostService.delete(10L, 2L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_NOT_FOUND);
    }

    @Test
    void deleteThrowsWhenPostIsDeleted() throws Exception {
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        post.softDelete();

        when(freePostRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> freePostService.delete(10L, 2L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.POST_DELETED);
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

    private FreePostDetailResponse updatePost(Long requesterId, List<String> imageUrls, Integer mainImageIndex) {
        return freePostService.update(
                requesterId,
                10L,
                "수정 제목",
                "수정 본문",
                imageUrls,
                mainImageIndex
        );
    }
}
