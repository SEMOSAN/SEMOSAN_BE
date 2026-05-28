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
 * 트래킹 진행 중 GPS 거리 누적값을 보고, 사전에 결정된 마일스톤 거리에 도달하면
 * 사용자에게 두 종류의 알림을 트리거한다.
 *
 * 1) 사진 마일스톤(photo) — 코스 4컷 / 자유 6컷.
 *    ±10% 윈도우 진입 시 OPEN, 이탈 시 CLOSED. 채널: WebSocket(/topic/.../photo-window) + FCM OPEN 시점만.
 *
 * 2) 정상 도달(summit) — 코스 거리 50% 지점 도달 시 1회 (자유 기록은 skip).
 *    코스 정상 좌표를 정확히 식별 못해 임시 정책으로 "코스 절반" 을 정상 근처로 본다.
 *    채널: WebSocket(/topic/.../summit) + FCM. Redis Set 으로 1회 idempotent 보장.
 *
 * TODO: 다중 인스턴스 동시성 — 같은 마일스톤에 대해 두 인스턴스가 동시 OPEN 발송할 가능성.
 *       Lua 스크립트 또는 분산 락으로 보강 필요.
 * TODO: 푸시 본문 단위 — 현재 m 단위 정수. 코스 마일스톤이 정수가 아닐 때 km 단위 포맷 고려.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingMilestoneTriggerService {

    private static final double TOLERANCE_RATIO = 0.10;
    private static final Duration TTL = Duration.ofHours(24);
    /** 코스 모드의 사진 마일스톤 개수 (1/4, 2/4, 3/4, 4/4). TrackingMilestoneCalculator 와 동기 유지. */
    private static final int COURSE_MILESTONE_COUNT = 4;

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
                sendOpen(sessionId, userId, i, mi, milestones.size());
                redisTemplate.opsForSet().add(openedKey(sessionId), idxStr);
                redisTemplate.expire(openedKey(sessionId), TTL);
            }
            if (isOpened && !isClosed && distanceTotal > exit) {
                sendClosed(sessionId, i, mi);
                redisTemplate.opsForSet().add(closedKey(sessionId), idxStr);
                redisTemplate.expire(closedKey(sessionId), TTL);
            }
        }

        // 코스 모드(마일스톤 4개)일 때만 정상(=코스 절반 지점) 알림 평가.
        // 마지막 마일스톤(4/4)이 곧 코스 distance 이므로 그걸 courseDistance 로 사용.
        if (milestones.size() == COURSE_MILESTONE_COUNT) {
            evaluateSummit(sessionId, userId, distanceTotal, milestones.get(milestones.size() - 1));
        }
    }

    private void sendOpen(Long sessionId, Long userId, int idx, double mi, int milestonesSize) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("milestoneIndex", idx);
        payload.put("milestoneDistance", mi);
        payload.put("status", "OPEN");
        payload.put("openedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(photoTopic(sessionId), payload);
        log.info("Photo window OPEN: sessionId={} idx={} milestone={}m", sessionId, idx, (int) Math.round(mi));

        try {
            int distanceMeters = (int) Math.round(mi);
            String body = courseModeBody(milestonesSize, idx, distanceMeters);
            notificationService.send(
                    userId,
                    NotificationType.TRACKING_PHOTO_MILESTONE,
                    Map.of("distance", distanceMeters),
                    body
            );
        } catch (Exception e) {
            // FCM 발송 실패가 WebSocket OPEN 자체를 막아선 안 됨 — 로그만 남기고 진행
            log.warn("Failed to send TRACKING_PHOTO_MILESTONE FCM: sessionId={} idx={} mi={} err={}",
                    sessionId, idx, mi, e.getMessage());
        }
    }

    /**
     * 코스 모드(4컷) 일 때만 마일스톤 idx 별로 본문을 반환한다.
     * 자유 기록(6컷) 등 코스 모드가 아닐 땐 기존 distance 기반 문구로 fallback 한다.
     */
    private static String courseModeBody(int milestonesSize, int milestoneIdx, int distanceMeters) {
        if (milestonesSize != COURSE_MILESTONE_COUNT) {
            return distanceMeters + "m 돌파! 인증 사진을 남겨보세요!";
        }
        return switch (milestoneIdx) {
            case 0 -> "정상 도착_1/4 완료_눌러서 인증 남기기";
            case 1 -> "정상 도착_절반 돌파_눌러서 인증 남기기";
            case 2 -> "정상 도착_마지막 1/4_눌러서 인증 남기기";
            case 3 -> "정상 도착_완료_진짜최종_눌러서 인증하기";
            default -> distanceMeters + "m 돌파! 인증 사진을 남겨보세요!";
        };
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

    /**
     * 코스 거리 50% 지점 도달 시 1회 정상 알림을 발송한다.
     *  - 자유 기록(session.course == null) 인 경우 정상 개념이 없으니 호출자에서 courseDistance=0 으로 넘기면 skip.
     *  - Redis SADD 의 반환값으로 idempotent — 두 인스턴스가 동시 호출해도 한 인스턴스만 발송.
     *  - WebSocket /topic/tracking/{sessionId}/summit + FCM 둘 다 발송.
     */
    public void evaluateSummit(Long sessionId, Long userId, double distanceTotal, double courseDistance) {
        if (courseDistance <= 0) {
            return;
        }
        double halfwayMark = courseDistance / 2.0;
        if (distanceTotal < halfwayMark) {
            return;
        }
        String key = summitNotifiedKey(sessionId);
        Long added = redisTemplate.opsForSet().add(key, "1");
        if (added == null || added == 0L) {
            // 이미 다른 호출에서 보낸 상태 — silent skip.
            // EXPIRE 도 함께 skip 해야 50% 통과 후 매 GPS 점마다 TTL 이 리셋되는 핫패스 부하를 막을 수 있다.
            return;
        }
        redisTemplate.expire(key, TTL);
        sendSummit(sessionId, userId, halfwayMark);
    }

    private void sendSummit(Long sessionId, Long userId, double halfwayMark) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("halfwayMark", halfwayMark);
        payload.put("reachedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(summitTopic(sessionId), payload);
        log.info("Summit reached: sessionId={} userId={} halfwayMark={}m", sessionId, userId, (int) Math.round(halfwayMark));

        try {
            notificationService.send(
                    userId,
                    NotificationType.TRACKING_SUMMIT_REACHED,
                    Map.of()
            );
        } catch (Exception e) {
            // FCM 실패가 WebSocket summit 자체를 막아선 안 됨 — 로그만 남기고 진행
            log.warn("Failed to send TRACKING_SUMMIT_REACHED FCM: sessionId={} err={}", sessionId, e.getMessage());
        }
    }

    private static String summitNotifiedKey(Long sessionId) {
        return "tracking:session:" + sessionId + ":summit:notified";
    }

    private static String summitTopic(Long sessionId) {
        return "/topic/tracking/" + sessionId + "/summit";
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
