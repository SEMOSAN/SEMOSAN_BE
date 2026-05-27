package com.semosan.api.domain.community.post.service;

import com.semosan.api.common.alert.DiscordAlertClient;
import com.semosan.api.common.alert.dto.DiscordEmbed;
import com.semosan.api.common.alert.dto.DiscordMessage;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.FreePostReport;
import com.semosan.api.domain.community.post.enums.FreePostReportReason;
import com.semosan.api.domain.community.post.repository.FreePostReportRepository;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreePostReportService {

    private static final String REPORT_UNIQUE_CONSTRAINT = "uk_free_post_reports_reporter_post";
    private static final int DISCORD_COLOR = 0xE74C3C;

    private final FreePostRepository freePostRepository;
    private final FreePostReportRepository freePostReportRepository;
    private final UserRepository userRepository;
    private final DiscordAlertClient discordAlertClient;

    @Transactional
    public FreePostReport report(Long reporterId, Long postId, FreePostReportReason reason) {
        User reporter = findReporterOrThrow(reporterId);
        FreePost post = findPostOrThrow(postId);

        if (post.getAuthor().getId().equals(reporterId)) {
            throw new GeneralException(ErrorStatus.FREE_POST_REPORT_SELF_NOT_ALLOWED);
        }
        if (freePostReportRepository.existsByReporter_IdAndPost_Id(reporterId, postId)) {
            throw new GeneralException(ErrorStatus.FREE_POST_REPORT_ALREADY_EXISTS);
        }

        FreePostReport report = FreePostReport.create(reporter, post, reason);
        try {
            FreePostReport saved = freePostReportRepository.saveAndFlush(report);
            DiscordMessage message = buildDiscordMessage(saved);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    discordAlertClient.sendReport(message);
                }
            });
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new GeneralException(ErrorStatus.FREE_POST_REPORT_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    private DiscordMessage buildDiscordMessage(FreePostReport report) {
        FreePost post = report.getPost();
        User reporter = report.getReporter();
        User author = post.getAuthor();

        String content = String.format(
                Locale.KOREA,
                "자유게시판 신고 접수\n게시글 ID: %d\n신고자: %d (%s)\n작성자: %d (%s)\n사유: %s\n게시글 제목: %s\n게시글 내용: %s\n작성 시각: %s",
                post.getId(),
                reporter.getId(),
                reporter.getNickname(),
                author.getId(),
                author.getNickname(),
                report.getReason().getLabel(),
                post.getTitle(),
                truncate(post.getContent(), 300),
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        return new DiscordMessage(
                "자유게시판 신고 접수",
                List.of(new DiscordEmbed("신고 상세", content, DISCORD_COLOR))
        );
    }

    private boolean isUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && REPORT_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(REPORT_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private User findReporterOrThrow(Long reporterId) {
        return userRepository.findByIdAndDeletedFalse(reporterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private FreePost findPostOrThrow(Long postId) {
        return freePostRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
