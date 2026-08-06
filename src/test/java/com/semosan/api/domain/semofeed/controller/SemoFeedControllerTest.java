package com.semosan.api.domain.semofeed.controller;

import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedCreateRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.service.SemoFeedEmojiService;
import com.semosan.api.domain.semofeed.service.SemoFeedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemoFeedControllerTest {

    @Mock
    private SemoFeedService semoFeedService;

    @Mock
    private SemoFeedEmojiService semoFeedEmojiService;

    @InjectMocks
    private SemoFeedController semoFeedController;

    @Test
    void createAndListPublicReturnSuccessResponses() {
        SemoFeedResponse response = response();
        PageRequest pageable = PageRequest.of(0, 100);
        when(semoFeedService.create(1L, "image.jpg")).thenReturn(response);
        when(semoFeedService.listPublic(pageable, 1L)).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        assertThat(semoFeedController.create(1L, new SemoFeedCreateRequest("image.jpg")).getBody().getData())
                .isSameAs(response);
        assertThat(semoFeedController.listPublic(1L, pageable).getBody().getData().content()).containsExactly(response);
    }

    @Test
    void emojiMineTogglePublicAndDeleteReturnSuccessResponses() {
        SemoFeedResponse response = response();
        SemoFeedEmojiToggleResponse toggle = new SemoFeedEmojiToggleResponse(SemoFeedEmojiType.FIRE, true, 1L);
        SemoFeedEmojiRequest request = new SemoFeedEmojiRequest(SemoFeedEmojiType.FIRE);
        when(semoFeedEmojiService.toggleWithCount(1L, 10L, SemoFeedEmojiType.FIRE)).thenReturn(toggle);
        when(semoFeedService.listMine(1L)).thenReturn(List.of(response));
        when(semoFeedService.togglePublic(1L, 10L)).thenReturn(true);

        assertThat(semoFeedController.toggleEmoji(1L, 10L, request).getBody().getData()).isSameAs(toggle);
        assertThat(semoFeedController.listMine(1L).getBody().getData()).containsExactly(response);
        assertThat(semoFeedController.togglePublic(1L, 10L).getBody().getData()).isTrue();
        assertThat(semoFeedController.delete(1L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.SEMOFEED_DELETE_SUCCESS.getHttpStatus());
        verify(semoFeedService).delete(1L, 10L);
    }

    private SemoFeedResponse response() {
        return new SemoFeedResponse(
                10L, 1L, "profile.jpg", "닉네임", "image.jpg", true,
                Map.of(SemoFeedEmojiType.FIRE, 1L),
                Map.of(SemoFeedEmojiType.FIRE, true),
                true
        );
    }
}
