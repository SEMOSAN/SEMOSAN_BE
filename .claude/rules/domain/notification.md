---
paths:
  - "src/main/java/**/domain/notification/**"
  - "src/main/java/**/common/fcm/**"
  - "src/main/java/**/semofeed/notification/**"
---
# notification 도메인

- 알림 발송 순서: 알림 엔티티 저장 → `NotificationCreatedEvent` 발행 → `NotificationEventListener`(`AFTER_COMMIT`) → `NotificationDispatcher.dispatch()`(`@Async`) → FCM.
  - 롤백되면 푸시도 나가지 않는 게 의도다. 리스너 phase를 바꾸거나 트랜잭션 안에서 FCM을 직접 호출하지 않는다.
- 다른 도메인에서 알림을 보낼 때는 FCM을 직접 호출하지 말고 이벤트를 발행한다.
- 회원 탈퇴 정리는 `NotificationUserWithdrawnListener`(`BEFORE_COMMIT`)가 탈퇴 트랜잭션 안에서 처리한다.
- `@Async` 메서드는 같은 클래스 안에서 호출하면 동기로 실행된다.
