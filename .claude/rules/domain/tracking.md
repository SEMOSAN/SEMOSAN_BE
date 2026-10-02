---
paths:
  - "src/main/java/**/domain/tracking/**"
  - "src/test/java/**/domain/tracking/**"
---
# tracking 도메인

흐름 상세: `.claude/docs/tracking-flow.md`

- GPS 좌표는 WebSocket(STOMP) → `TrackingGpsPublisher` → Redis Stream → `TrackingStreamConsumer` 경로로만 들어온다. REST로 포인트를 저장하는 경로를 새로 만들지 않는다.
- `TrackingStreamConsumer`의 버퍼(`ConcurrentHashMap<Long, Queue>`)는 인스턴스 메모리다. 버퍼를 건드리는 변경은 동시성(스케줄러, 소비 스레드, 종료 이벤트가 동시에 접근)을 반드시 검토한다.
- DB 저장은 반드시 `TrackingPointFlushService`(별도 빈)를 거친다. Consumer 안에 `@Transactional` 메서드를 만들면 self-invocation으로 무효화된다.
- flush 조건: 세션별 100건(`FLUSH_THRESHOLD`) 또는 10초 스케줄러. 세션 종료 시 `TrackingSessionTerminatedEvent`(AFTER_COMMIT)로 잔여 버퍼를 flush한다. 종료 경로를 추가하면 이 이벤트를 발행해야 포인트가 유실되지 않는다.
- 실시간 통계는 Redis Hash `tracking:session:{id}:stats` (`TrackingSessionStatsService`). 키 형식을 바꾸면 기존 세션과 호환성을 확인한다.
- STOMP 인증은 HTTP 필터가 아니라 `StompAuthChannelInterceptor`(CONNECT 프레임)에서 한다.
