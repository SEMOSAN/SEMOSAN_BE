# 실시간 GPS 트래킹 흐름

1. 클라이언트가 STOMP로 GPS 좌표 전송 → `TrackingGpsWebSocketController`
2. `TrackingGpsPublisher`가 Redis Stream(`tracking:gps`, env `TRACKING_STREAM_KEY`)에 publish
3. `TrackingStreamConsumer`(consumer group `TRACKING_CONSUMER_GROUP`)가 소비
   - `TrackingSessionStatsService`: Redis Hash `tracking:session:{id}:stats`에 거리, 시간 등 실시간 통계 갱신
   - 세션별 메모리 버퍼에 `PendingPointCommand` 적재
   - `TrackingMilestoneTriggerService`: 마일스톤 도달 판정
4. flush 조건
   - 세션 버퍼 100건 이상이면 즉시
   - `@Scheduled(fixedDelay = 10s)` 주기
   → `TrackingPointFlushService`(별도 빈, `@Transactional`) → `TrackingPointJdbcRepository` 배치 insert
5. 세션 종료 시 `TrackingSessionTerminatedEvent`(AFTER_COMMIT)로 잔여 버퍼를 100건 단위로 final flush

## 스케줄러
- `TrackingSessionExpiryScheduler`: 방치된 세션 만료
- `TrackingStreamTrimScheduler`: 스트림 길이 정리
- `TrackingPointCleanupScheduler`: 오래된 포인트 정리

## 알려진 한계
- 버퍼가 인스턴스 메모리라 flush 전에 프로세스가 죽으면 그 사이 포인트가 유실될 수 있다.
- Redis AOF 도입은 다중 인스턴스 구성이 선행돼야 해서 보류 중이다 (이슈 #310).
