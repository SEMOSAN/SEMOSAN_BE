package com.semosan.api.domain.community.post.entity;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    void softDeleteMarksPostDeleted() {
        FreePost post = FreePost.create(user(1L, "author"), "제목", "본문");

        post.softDelete();

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void isOwnedByRequiresAuthorAndMatchingUserId() {
        User author = user(1L, "author");
        FreePost post = FreePost.create(author, "제목", "본문");
        FreePost postWithoutAuthor = FreePost.create(author, "제목", "본문");
        ReflectionTestUtils.setField(postWithoutAuthor, "author", null);

        assertThat(post.isOwnedBy(1L)).isTrue();
        assertThat(post.isOwnedBy(2L)).isFalse();
        assertThat(post.isOwnedBy(null)).isFalse();
        assertThat(postWithoutAuthor.isOwnedBy(1L)).isFalse();
    }

    @Test
    void freePostUpdateChangesTitleAndContentThroughProtectedPostMethod() {
        FreePost post = FreePost.create(user(1L, "author"), "기존 제목", "기존 본문");

        post.update("수정 제목", "수정 본문");

        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 본문");
    }

    private User user(Long id, String nickname) {
        User user = User.createTestUser(nickname, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
