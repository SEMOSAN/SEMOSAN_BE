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
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final UserRepository userRepository;

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
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        FreePost post = FreePost.create(author, title, content);
        freePostRepository.save(post);

        List<PostImage> images = saveImages(post, imageUrls, mainImageIndex);

        return FreePostDetailResponse.from(post, images, 0L, 0L);
    }

    public Page<FreePostListResponse> getList(Pageable pageable) {
        return enrichWithCounts(freePostRepository.findAllByDeletedFalse(pageable));
    }

    public Page<FreePostListResponse> getMyList(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        return enrichWithCounts(freePostRepository.findByAuthorAndDeletedFalse(author, pageable));
    }

    @Transactional
    public FreePostDetailResponse getDetail(Long postId) {
        FreePost post = findActivePostOrThrow(postId);
        post.increaseViewCount();

        List<PostImage> images = postImageRepository.findByPostOrderBySortOrderAsc(post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostAndDeletedFalse(post);

        return FreePostDetailResponse.from(post, images, likeCount, commentCount);
    }

    @Transactional
    public void delete(Long postId, Long requesterId) {
        FreePost post = findActivePostOrThrow(postId);
        if (!post.getAuthor().getId().equals(requesterId)) {
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

        return posts.map(post -> FreePostListResponse.from(
                post,
                mainImageMap.get(post.getId()),
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
