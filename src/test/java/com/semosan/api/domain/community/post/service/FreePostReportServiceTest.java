package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.alert.DiscordAlertClient;
import com.semosan.api.common.alert.dto.DiscordMessage;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.FreePostReport;
import com.semosan.api.domain.community.post.enums.FreePostReportReason;
import com.semosan.api.domain.community.post.repository.FreePostReportRepository;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreePostReportServiceTest {

    @Mock
    private FreePostRepository freePostRepository;

    @Mock
    private FreePostReportRepository freePostReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscordAlertClient discordAlertClient;

    @InjectMocks
    private FreePostReportService freePostReportService;

    @Test
    void reportSavesReportAndSendsDiscordAlert() throws Exception {
        User reporter = user(1L, "reporter");
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");
        FreePostReport report = FreePostReport.create(reporter, post, FreePostReportReason.SPAM);

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(reporter));
        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(freePostReportRepository.existsByReporter_IdAndPost_Id(1L, 10L)).thenReturn(false);
        when(freePostReportRepository.saveAndFlush(any(FreePostReport.class))).thenReturn(report);

        FreePostReport saved = freePostReportService.report(1L, 10L, FreePostReportReason.SPAM);

        assertThat(saved.getReason()).isEqualTo(FreePostReportReason.SPAM);
        ArgumentCaptor<DiscordMessage> messageCaptor = ArgumentCaptor.forClass(DiscordMessage.class);
        verify(discordAlertClient).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().content()).contains("자유게시판 신고 접수");
        assertThat(messageCaptor.getValue().embeds().get(0).description()).contains("스팸");
    }

    @Test
    void reportThrowsWhenReportingOwnPost() throws Exception {
        User reporter = user(1L, "reporter");
        FreePost post = freePost(10L, reporter, "제목", "본문");

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(reporter));
        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> freePostReportService.report(1L, 10L, FreePostReportReason.SPAM))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FREE_POST_REPORT_SELF_NOT_ALLOWED);
        verify(freePostReportRepository, never()).saveAndFlush(any());
        verify(discordAlertClient, never()).send(any());
    }

    @Test
    void reportThrowsWhenDuplicateReportExists() throws Exception {
        User reporter = user(1L, "reporter");
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(reporter));
        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(freePostReportRepository.existsByReporter_IdAndPost_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> freePostReportService.report(1L, 10L, FreePostReportReason.SPAM))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FREE_POST_REPORT_ALREADY_EXISTS);
        verify(freePostReportRepository, never()).saveAndFlush(any());
        verify(discordAlertClient, never()).send(any());
    }

    @Test
    void reportThrowsWhenUniqueConstraintFailsDuringRace() throws Exception {
        User reporter = user(1L, "reporter");
        User author = user(2L, "author");
        FreePost post = freePost(10L, author, "제목", "본문");

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(reporter));
        when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(freePostReportRepository.existsByReporter_IdAndPost_Id(1L, 10L)).thenReturn(false);
        when(freePostReportRepository.saveAndFlush(any(FreePostReport.class)))
                .thenThrow(uniqueViolation());

        assertThatThrownBy(() -> freePostReportService.report(1L, 10L, FreePostReportReason.SPAM))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FREE_POST_REPORT_ALREADY_EXISTS);
    }

    private DataIntegrityViolationException uniqueViolation() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate",
                new SQLException("unique violation"),
                "uk_free_post_reports_reporter_post"
        );
        return new DataIntegrityViolationException("duplicate", cause);
    }

    private User user(Long id, String oauthId) {
        User user = User.createTestUser(oauthId, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", oauthId);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now());
        return user;
    }

    private FreePost freePost(Long id, User author, String title, String content) throws Exception {
        Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
        constructor.setAccessible(true);
        FreePost post = constructor.newInstance(author, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }
}
