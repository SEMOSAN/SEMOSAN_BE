package com.semosan.api.domain.community.post.dto;

import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FreePostListResponseTest {

    @Test
    void fromReturnsPreviewAndExtraImageCount() {
        FreePost post = freePost(10L, user(1L, "author"), "제목", "가".repeat(101));

        FreePostListResponse result = FreePostListResponse.from(
                post,
                "https://example.com/main.png",
                3,
                5L,
                7L
        );

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.author().nickname()).isEqualTo("author");
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.contentPreview()).isEqualTo("가".repeat(100));
        assertThat(result.thumbnailUrl()).isEqualTo("https://example.com/main.png");
        assertThat(result.extraImageCount()).isEqualTo(2);
        assertThat(result.likeCount()).isEqualTo(5L);
        assertThat(result.commentCount()).isEqualTo(7L);
    }

    @Test
    void fromReturnsNullPreviewAndExtraImageCountWhenContentOrImageCountMissing() {
        FreePost post = freePost(10L, user(1L, "author"), "제목", null);

        FreePostListResponse nullCountResult = FreePostListResponse.from(post, null, null, 0L, 0L);
        FreePostListResponse oneImageResult = FreePostListResponse.from(post, null, 1, 0L, 0L);

        assertThat(nullCountResult.contentPreview()).isNull();
        assertThat(nullCountResult.extraImageCount()).isNull();
        assertThat(oneImageResult.extraImageCount()).isNull();
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private FreePost freePost(Long id, User author, String title, String content) {
        FreePost post = FreePost.create(author, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
