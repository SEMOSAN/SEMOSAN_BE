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
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreePostService {

    private final FreePostRepository freePostRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostAccessPolicy postAccessPolicy;
    private final UserReader userReader;

    @Transactional
    public FreePostDetailResponse create(
            Long authorId,
            String title,
            String content,
            List<String> imageUrls,
            Integer mainImageIndex
    ) {
        if (content == null || content.isBlank()) {
            throw new GeneralException(ErrorStatus.POST_CONTENT_REQUIRED);
        }
        User author = userReader.findActiveUserById(authorId);

        FreePost post = FreePost.create(author, title, content);
        freePostRepository.save(post);

        List<PostImage> images = saveImages(post, imageUrls, mainImageIndex);

        return FreePostDetailResponse.of(post, images, 0L, 0L, false);
    }

    public Page<FreePostListResponse> getList(Long viewerId, Pageable pageable) {
        return enrichWithCounts(freePostRepository.findVisibleByViewerId(viewerId, pageable));
    }

    public Page<FreePostListResponse> search(Long viewerId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return enrichWithCounts(freePostRepository.searchByKeyword(viewerId, keyword.strip(), unsorted));
    }

    public Page<FreePostListResponse> getMyList(Long authorId, Pageable pageable) {
        User author = userReader.findActiveUserById(authorId);
        return enrichWithCounts(freePostRepository.findByAuthorAndDeletedFalse(author, pageable));
    }

    @Transactional
    public FreePostDetailResponse getDetail(Long viewerId, Long postId) {
        FreePost post = findActivePostOrThrow(postId);
        postAccessPolicy.validateReadable(viewerId, post);
        post.increaseViewCount();

        List<PostImage> images = postImageRepository.findByPostOrderBySortOrderAsc(post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostAndDeletedFalse(post);
        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(post.getId(), viewerId);

        return FreePostDetailResponse.of(post, images, likeCount, commentCount, likedByMe);
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        FreePost post = findActivePostOrThrow(postId);
        if (!post.isOwnedBy(requesterId)) {
            throw new GeneralException(ErrorStatus.POST_FORBIDDEN);
        }
        post.softDelete();
    }

    private List<PostImage> saveImages(FreePost post, List<String> imageUrls, Integer mainImageIndex) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        int main = (mainImageIndex != null) ? mainImageIndex : 0;
        if (main < 0 || main >= imageUrls.size()) {
            throw new GeneralException(ErrorStatus.POST_IMAGE_INDEX_INVALID);
        }
        List<PostImage> saved = new java.util.ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            saved.add(postImageRepository.save(PostImage.create(post, imageUrls.get(i), i, i == main)));
        }
        return saved;
    }

    private Page<FreePostListResponse> enrichWithCounts(Page<FreePost> posts) {
        if (posts.isEmpty()) {
            return posts.map(p -> null);
        }
        List<Long> postIds = posts.getContent().stream().map(FreePost::getId).toList();

        Map<Long, Long> likeCountMap = postLikeRepository.countByPostIdsGrouped(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, Long> commentCountMap = commentRepository.countByPostIdsGrouped(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, String> mainImageMap = postImageRepository.findMainImagesByPostIds(postIds).stream()
                .collect(Collectors.toMap(img -> img.getPost().getId(), PostImage::getImageUrl));
        Map<Long, Long> imageCountMap = postImageRepository.countByPostIdsGrouped(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return posts.map(post -> FreePostListResponse.from(
                post,
                mainImageMap.get(post.getId()),
                imageCountMap.getOrDefault(post.getId(), 0L).intValue(),
                likeCountMap.getOrDefault(post.getId(), 0L),
                commentCountMap.getOrDefault(post.getId(), 0L)
        ));
    }

    private FreePost findActivePostOrThrow(Long postId) {
        FreePost post = freePostRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorStatus.POST_DELETED);
        }
        return post;
    }
}
