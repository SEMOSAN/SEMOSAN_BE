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
 * 1) 사진 마일스톤(photo) — 코스 4컷 / 자유 4컷.
 *    ±10% 윈도우 진입 시 OPEN, 이탈 시 CLOSED. 채널: WebSocket(/topic/.../photo-window) + FCM OPEN 시점만.
 *
 * 2) 정상 도달(summit) — 시작점→정상 누적 거리 도달 시 1회 (자유 기록은 skip).
 *    정상 좌표가 없는 코스만 종전 정책대로 "코스 절반" 을 정상 근처로 본다.
 *    채널: WebSocket(/topic/.../summit) + FCM. Redis Set 으로 1회 idempotent 보장.
 *
 * TODO: 푸시 본문 단위 — 현재 m 단위 정수. 코스 마일스톤이 정수가 아닐 때 km 단위 포맷 고려.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingMilestoneTriggerService {

    private static final double TOLERANCE_RATIO = 0.10;
    private static final Duration TTL = Duration.ofHours(24);
    /** 자유 기록엔 정상이 없다는 뜻의 sentinel. 키가 아예 없는 구버전 세션과 구분하려고 명시적으로 저장한다. */
    private static final String SUMMIT_MARK_NONE = "none";
    /**
     * 이 변경 배포 전에 생성된 세션의 코스 모드 판별값. 그때는 자유 기록이 6컷이라 4컷이면 곧 코스였다.
     * 신규 세션은 summit mark 키로 판별하므로 {@link #resolveSummitMark} 의 구버전 분기에서만 쓰인다.
     */
    private static final int LEGACY_COURSE_MILESTONE_COUNT = 4;

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final TrackingMilestoneCalculator milestoneCalculator;

    /** 세션 생성 시 1회 호출 — 마일스톤 거리 리스트와 정상 알림 임계 거리를 Redis 에 저장. */
    public void initializeMilestones(TrackingSession session) {
        TrackingMilestoneCalculator.MilestonePlan plan = milestoneCalculator.calculate(session);
        if (plan.milestones().isEmpty()) {
            return;
        }
        String key = milestonesKey(session.getId());
        String value = plan.milestones().stream().map(String::valueOf).collect(Collectors.joining(","));
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, TTL);

        String markKey = summitMarkKey(session.getId());
        redisTemplate.opsForValue().set(markKey,
                plan.summitMark() == null ? SUMMIT_MARK_NONE : String.valueOf(plan.summitMark()));
        redisTemplate.expire(markKey, TTL);
    }

    /** GPS Consumer 가 매 점 처리 직후 호출. distanceTotal 은 누적 거리(m). */
    public void evaluate(Long sessionId, Long userId, double distanceTotal) {
        List<Double> milestones = loadMilestones(sessionId);
        if (milestones.isEmpty()) {
            return;
        }
        // summitMark 는 정상 알림 임계 거리이자 코스 모드 판별 기준이다 (자유 기록이면 null).
        Double summitMark = resolveSummitMark(sessionId, milestones);
        boolean courseMode = summitMark != null;
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
                Long added = redisTemplate.opsForSet().add(openedKey(sessionId), idxStr);
                if (added != null && added == 1L) {
                    redisTemplate.expire(openedKey(sessionId), TTL);
                    sendOpen(sessionId, userId, i, mi, courseMode);
                }
            }
            if (isOpened && !isClosed && distanceTotal > exit) {
                Long added = redisTemplate.opsForSet().add(closedKey(sessionId), idxStr);
                if (added != null && added == 1L) {
                    redisTemplate.expire(closedKey(sessionId), TTL);
                    sendClosed(sessionId, i, mi);
                }
            }
        }

        // 코스 모드일 때만 정상 알림 평가. 정상 좌표가 있으면 4/4 마일스톤이 곧 정상 지점이라
        // photo 4/4 와 같은 지점에서 함께 발송된다.
        if (courseMode) {
            evaluateSummit(sessionId, userId, distanceTotal, summitMark);
        }
    }

    /**
     * 정상 알림 임계 거리(m)를 돌려준다. null 이면 정상 알림이 없는 자유 기록이다.
     *
     * 마크 키가 아예 없는 세션은 이 변경 배포 전에 생성돼 진행 중인 세션이다. 그때는 마일스톤 4개가
     * 곧 코스 모드였고 정상을 코스 거리의 절반으로 봤으므로, 그 규칙 그대로 계산해 알림 공백을 막는다.
     */
    private Double resolveSummitMark(Long sessionId, List<Double> milestones) {
        String raw = redisTemplate.opsForValue().get(summitMarkKey(sessionId));
        if (raw == null) {
            return milestones.size() == LEGACY_COURSE_MILESTONE_COUNT
                    ? milestones.get(milestones.size() - 1) / 2.0
                    : null;
        }
        if (raw.isBlank() || SUMMIT_MARK_NONE.equals(raw)) {
            return null;
        }
        return Double.parseDouble(raw);
    }

    private void sendOpen(Long sessionId, Long userId, int idx, double mi, boolean courseMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("milestoneIndex", idx);
        payload.put("milestoneDistance", mi);
        payload.put("status", "OPEN");
        payload.put("openedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(photoTopic(sessionId), (Object) payload);
        log.info("Photo window OPEN: sessionId={} idx={} milestone={}m", sessionId, idx, (int) Math.round(mi));

        try {
            int distanceMeters = (int) Math.round(mi);
            String body = courseModeBody(courseMode, idx, distanceMeters);
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
     * 코스 모드일 때만 마일스톤 idx 별로 본문을 반환한다.
     * 자유 기록은 정상 개념이 없으므로 distance 기반 문구로 fallback 한다.
     *
     * 판별 기준이 마일스톤 개수가 아니라 코스 모드 여부인 이유: 자유 기록도 4컷이라
     * 개수로는 코스와 구분되지 않아 자유 기록에 "정상 도착" 문구가 나가버린다.
     */
    private static String courseModeBody(boolean courseMode, int milestoneIdx, int distanceMeters) {
        if (!courseMode) {
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
        messagingTemplate.convertAndSend(photoTopic(sessionId), (Object) payload);
        log.info("Photo window CLOSED: sessionId={} idx={} milestone={}m", sessionId, idx, (int) Math.round(mi));
    }

    /**
     * 정상 지점 도달 시 1회 정상 알림을 발송한다.
     *  - summitMark 는 코스 시작점부터 정상까지의 누적 거리(m). 정상 좌표가 없는 코스는
     *    종전 정책대로 코스 거리의 절반이 들어온다.
     *  - 자유 기록은 정상 개념이 없으니 호출자에서 skip 한다.
     *  - Redis SADD 의 반환값으로 idempotent — 두 인스턴스가 동시 호출해도 한 인스턴스만 발송.
     *  - WebSocket /topic/tracking/{sessionId}/summit + FCM 둘 다 발송.
     */
    public void evaluateSummit(Long sessionId, Long userId, double distanceTotal, double summitMark) {
        if (summitMark <= 0) {
            return;
        }
        if (distanceTotal < summitMark) {
            return;
        }
        String key = summitNotifiedKey(sessionId);
        Long added = redisTemplate.opsForSet().add(key, "1");
        if (added == null || added == 0L) {
            // 이미 다른 호출에서 보낸 상태 — silent skip.
            // EXPIRE 도 함께 skip 해야 정상 통과 후 매 GPS 점마다 TTL 이 리셋되는 핫패스 부하를 막을 수 있다.
            return;
        }
        redisTemplate.expire(key, TTL);
        sendSummit(sessionId, userId, summitMark);
    }

    private void sendSummit(Long sessionId, Long userId, double summitMark) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // 페이로드 키는 의미가 "코스 절반" 에서 "정상까지 거리" 로 바뀌었지만,
        // 이미 배포된 클라이언트가 읽고 있어 이름은 그대로 둔다.
        payload.put("halfwayMark", summitMark);
        payload.put("reachedAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(summitTopic(sessionId), (Object) payload);
        log.info("Summit reached: sessionId={} userId={} summitMark={}m", sessionId, userId, (int) Math.round(summitMark));

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

    /**
     * 앱 재실행 후 복원용 — 현재 마일스톤 진행 상태 스냅샷.
     *
     * OPEN/CLOSED 는 WebSocket push 로만 전달되고 어디에도 저장되지 않으므로,
     * 앱이 꺼져 있는 동안 발생한 이벤트는 클라이언트가 영영 받지 못한다.
     * 그 공백을 메우기 위해 Redis 에 남아 있는 상태를 그대로 읽어 돌려준다.
     *
     * 마일스톤이 초기화되지 않은 세션(계산 결과가 비어 있던 경우)이나 TTL 24h 가 지난
     * 세션은 milestones 가 빈 목록으로 나간다.
     */
    public MilestoneState getMilestoneState(Long sessionId) {
        return new MilestoneState(
                loadMilestones(sessionId),
                toSortedIndexes(membersOrEmpty(openedKey(sessionId))),
                toSortedIndexes(membersOrEmpty(closedKey(sessionId))),
                !membersOrEmpty(summitNotifiedKey(sessionId)).isEmpty()
        );
    }

    /**
     * @param milestones     마일스톤 거리 목록 (m)
     * @param openedIndexes  촬영 창이 열린 적 있는 마일스톤 인덱스
     * @param closedIndexes  촬영 창이 닫힌 마일스톤 인덱스. openedIndexes - closedIndexes 가 현재 열린 창.
     * @param summitNotified 정상 도달 알림 발송 여부
     */
    public record MilestoneState(
            List<Double> milestones,
            List<Integer> openedIndexes,
            List<Integer> closedIndexes,
            boolean summitNotified
    ) {
    }

    /** Redis Set 은 순서를 보장하지 않으므로 인덱스로 파싱 후 정렬해 돌려준다. */
    private static List<Integer> toSortedIndexes(Set<String> members) {
        return members.stream()
                .map(Integer::parseInt)
                .sorted()
                .toList();
    }

    private static String summitMarkKey(Long sessionId) {
        return "tracking:session:" + sessionId + ":summit:mark";
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
