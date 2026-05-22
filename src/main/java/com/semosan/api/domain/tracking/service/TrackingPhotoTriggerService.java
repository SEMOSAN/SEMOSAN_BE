package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 트래킹 진행 중 GPS 거리 누적값을 보고, 사전에 결정된 마일스톤 거리 ±10% 윈도우에
 * 진입/이탈할 때 다음 동작을 수행한다.
 *  - 진입(OPEN): WebSocket OPEN 메시지 + FCM 푸시 ("{distance}m 돌파! 인증 사진을 남겨보세요!")
 *  - 이탈(CLOSED): WebSocket CLOSED 메시지 (FCM 없음)
 * 마일스톤 도달 상태는 Redis Set 으로 idempotent 하게 관리.
 *
 * TODO: 다중 인스턴스 동시성 — 같은 마일스톤에 대해 두 인스턴스가 동시 OPEN 발송할 가능성.
 *       Lua 스크립트 또는 분산 락으로 보강 필요.
 * TODO: 푸시 본문 단위 — 현재 m 단위 정수. 코스 마일스톤이 정수가 아닐 때 km 단위 포맷 고려.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingPhotoTriggerService {

    private static final double TOLERANCE_RATIO = 0.10;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final TrackingMilestoneCalculator milestoneCalculator;

    /** 세션 생성 시 1회 호출 — 마일스톤 거리 리스트를 Redis 에 저장. */
    public void initializeMilestones(TrackingSession session) {
        List<Double> milestones = milestoneCalculator.calculate(session);
        if (milestones.isEmpty()) {
            return;
        }
        String key = milestonesKey(session.getId());
        String value = milestones.stream().map(String::valueOf).collect(Collectors.joining(","));
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, TTL);
    }

    /** GPS Consumer 가 매 점 처리 직후 호출. distanceTotal 은 누적 거리(m). */
    public void evaluate(Long sessionId, Long userId, double distanceTotal) {
        List<Double> milestones = loadMilestones(sessionId);
        if (milestones.isEmpty()) {
            return;
        }
        Set<String> opened = membersOrEmpty(openedKey(sessionId));
        Set<String> closed = membersOrEmpty(closedKey(sessionId));

        for (int i = 0; i < milestones.size(); i++) {
            String idxStr = String.valueOf(i);
            double mi = milestones.get(i);
            double entry = mi * (1 - TOLERANCE_RATIO);
            double exit = mi * (1 + TOLERANCE_RATIO);
            boolean isOpened = opened.contains(idxStr);
            boolean isClosed = closed.contains(idxStr);

            if (!isOpened && distanceTotal >= entry && distanceTotal <= exit) {
                sendOpen(sessionId, userId, i, mi);
                redisTemplate.opsForSet().add(openedKey(sessionId), idxStr);
                redisTemplate.expire(openedKey(sessionId), TTL);
            }
            if (isOpened && !isClosed && distanceTotal > exit) {
                sendClosed(sessionId, i, mi);
                redisTemplate.opsForSet().add(closedKey(sessionId), idxStr);
                redisTemplate.expire(closedKey(sessionId), TTL);
            }
        }
    }

    private void sendOpen(Long sessionId, Long userId, int idx, double mi) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("milestoneIndex", idx);
        payload.put("milestoneDistance", mi);
        payload.put("status", "OPEN");
        payload.put("openedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(photoTopic(sessionId), payload);
        log.info("Photo window OPEN: sessionId={} idx={} milestone={}m", sessionId, idx, (int) Math.round(mi));

        try {
            notificationService.send(
                    userId,
                    NotificationType.TRACKING_PHOTO_MILESTONE,
                    Map.of("distance", (int) Math.round(mi))
            );
        } catch (Exception e) {
            // FCM 발송 실패가 WebSocket OPEN 자체를 막아선 안 됨 — 로그만 남기고 진행
            log.warn("Failed to send TRACKING_PHOTO_MILESTONE FCM: sessionId={} idx={} mi={} err={}",
                    sessionId, idx, mi, e.getMessage());
        }
    }

    private void sendClosed(Long sessionId, int idx, double mi) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("milestoneIndex", idx);
        payload.put("milestoneDistance", mi);
        payload.put("status", "CLOSED");
        payload.put("closedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(photoTopic(sessionId), payload);
        log.info("Photo window CLOSED: sessionId={} idx={} milestone={}m", sessionId, idx, (int) Math.round(mi));
    }

    private List<Double> loadMilestones(Long sessionId) {
        String raw = redisTemplate.opsForValue().get(milestonesKey(sessionId));
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Double::parseDouble)
                .toList();
    }

    private Set<String> membersOrEmpty(String key) {
        Set<String> members = redisTemplate.opsForSet().members(key);
        return members == null ? Set.of() : members;
    }

    private static String milestonesKey(Long sessionId) {
        return "tracking:session:" + sessionId + ":milestones";
    }

    private static String openedKey(Long sessionId) {
        return "tracking:session:" + sessionId + ":photo:opened";
    }

    private static String closedKey(Long sessionId) {
        return "tracking:session:" + sessionId + ":photo:closed";
    }

    private static String photoTopic(Long sessionId) {
        return "/topic/tracking/" + sessionId + "/photo-window";
    }
}
