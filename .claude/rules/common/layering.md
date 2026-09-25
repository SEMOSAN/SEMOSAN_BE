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
