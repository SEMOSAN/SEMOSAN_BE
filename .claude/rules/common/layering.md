---
paths:
  - "src/main/java/**"
---
# 계층·코드 규칙

- 도메인 패키지는 `controller → service → repository` 방향으로만 의존한다. 컨트롤러가 repository를 직접 쓰지 않는다.
- 비즈니스 오류는 `throw new GeneralException(ErrorStatus.XXX)`로 던진다. 새 코드는 `ErrorStatus`/`SuccessStatus` enum에 추가한다.
  - 코드 형식: `<도메인약어>_<HTTP상태>_<순번>` (예: `POST_404_1`, `TRK_200_9`). 같은 도메인의 기존 약어와 마지막 순번을 먼저 확인한다.
- 새 추상화보다 기존 서비스, 헬퍼, DTO를 먼저 재사용한다. 같은 검증이나 조립 로직을 여러 서비스에 복제하지 않는다.
- `@Transactional`, `@Async`는 같은 클래스 안에서 호출(self-invocation)하면 프록시를 거치지 않아 무시된다. 필요하면 별도 빈으로 분리한다 (예: `TrackingPointFlushService`, `AsyncNotificationDispatcher`).

## Entity
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Setter` 금지. 상태 변경은 의미 있는 도메인 메서드로 한다.
- 생성은 `@Builder` 또는 정적 팩토리(`create`/`of`)로 하고, `BaseEntity`를 상속한다.
- enum은 엔티티 안에 중첩하지 않고 도메인의 `enums/` 패키지에 둔다 (예: `domain/tracking/enums/TrackingSessionStatus`).
- enum 컬럼은 `@Enumerated(EnumType.STRING)`을 쓴다. 값을 추가하면 DB CHECK 제약도 마이그레이션으로 갱신한다.

## Repository
- 기본은 파생 쿼리와 JPQL `@Query`를 쓴다.
- PostGIS 함수가 필요하면 native query를 쓴다.
- 대량 insert는 JdbcTemplate을 쓴다 (예: `TrackingPointJdbcRepository`).

## Service 트랜잭션
- DB를 쓰는 서비스는 클래스에 `@Transactional(readOnly = true)`를 두고, 쓰기 메서드에만 `@Transactional`을 붙인다.
- DB를 쓰지 않는 서비스(Redis, MinIO, 외부 API 전용)는 붙이지 않는다.
- 기존 코드 중 이 방식이 아닌 서비스는 일괄 수정하지 않는다. 새 코드와 `/refactor` 대상에만 적용한다.
