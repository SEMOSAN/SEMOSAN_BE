# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication

- 한국어로 답변한다.
- "확인해줘", "봐줘", "뭐가 문제야" 같은 표현은 분석만 요청하는 것으로 간주하고 코드를 수정하지 않는다.
- "수정해줘", "고쳐줘", "반영해줘", "만들어줘", "추가해줘", "삭제해줘" 같은 명시적 요청이 있을 때만 파일을 편집한다.
- 수정 전에 어떤 파일을 왜 건드릴지 먼저 설명하고, 명시적 승인을 기다린다.

## Workflow

작업은 항상 아래 순서로 진행한다.

1. **분석**: 관련 코드·설정·로그를 먼저 파악한다.
2. **제안**: 원인과 해결 방향을 설명한다.
3. **비교**: 대안이 있으면 트레이드오프를 비교해 추천안을 제시한다.
4. **작업**: 사용자 승인 후에만 실제로 파일을 수정한다.

그 외 규칙:

- 커밋은 항상 사용자가 직접 한다. Claude는 스스로 커밋하지 않는다.
- 작업이 끝나면 커밋 메시지를 추천한다 (커밋 자체는 하지 않는다).
- PR을 생성할 때는 변경 타입에 맞는 라벨(`bug`/`enhancement`/`refactor`/`test`/`documentation`/`chore`)을 지정하고, 담당자(assignee)는 `gh pr create --assignee @me`로 현재 작업 중인 사람(gh CLI 인증 계정)을 지정한다.

## Build & Test Commands

```bash
./gradlew build                                        # 전체 빌드
./gradlew test                                         # 전체 테스트
./gradlew test --tests <fully.qualified.TestClass>     # 단일 클래스 테스트
./gradlew bootRun                                      # 애플리케이션 실행 (local 프로파일)
```

로컬 인프라(PostgreSQL, Redis, MinIO)가 없으면 테스트가 실패할 수 있다. 인프라 부재로 인한 실패와 코드 문제로 인한 실패를 명확히 구분해서 보고한다.

## Architecture Overview

### Package Structure

```text
com.semosan.api
├── common/          # 공통 인프라 (응답, 예외, JWT, 설정, FCM, 알림 공통)
└── domain/          # 도메인별 비즈니스 로직
    ├── auth/        # JWT 로그인/로그아웃/토큰 재발급, 회원탈퇴
    ├── oauth/       # 카카오·애플 소셜 로그인
    ├── user/        # 사용자 프로필, 온보딩, 알림 설정
    ├── mountain/    # 산 정보, 코스, 좋아요, 지도 검색 (PostGIS)
    ├── hiking/      # 등산 기록 조회
    ├── tracking/    # 실시간 GPS 트래킹 (WebSocket STOMP + Redis Stream)
    ├── community/   # 자유게시글·기록게시글, 댓글, 좋아요
    ├── image/       # MinIO presigned URL 발급
    ├── notification/# FCM 토큰, 알림 엔티티, 이벤트
    └── review/      # 산 리뷰
```

### Layer Convention

각 도메인은 `controller → service → repository` 계층을 따른다.  
Swagger 문서는 컨트롤러와 분리된 `controller/docs/<Name>ControllerDocs` 인터페이스에 작성하고, 컨트롤러가 이 인터페이스를 구현한다.

DTO는 다음 규칙을 따른다.
- 요청: `dto/request/` 하위 또는 `dto/` 직하위에 `*Request.java`
- 응답: `dto/response/` 하위 또는 `dto/` 직하위에 `*Response.java`
- 서비스 간 전달 커맨드: `dto/command/` 하위에 `*Command.java`

### API Response Convention

모든 컨트롤러는 `ApiResponse<T>`로 응답을 반환한다.

```java
// 데이터 없는 성공
return ApiResponse.success(SuccessStatus.XXX);

// 데이터 포함 성공
return ApiResponse.success(SuccessStatus.XXX, data);

// 오류
throw new GeneralException(ErrorStatus.XXX);
```

`SuccessStatus`와 `ErrorStatus` 모두 `BaseStatus` 인터페이스를 구현하는 enum이다.  
새 에러 코드는 `ErrorStatus`에, 성공 코드는 `SuccessStatus`에 추가한다.

### Authentication

- Stateless JWT 방식. `JwtFilter`가 `Authorization: Bearer <token>` 헤더를 검증한다.
- WebSocket(STOMP) 연결은 HTTP 필터를 통과(`/ws/tracking/**` permitAll)하고, `StompAuthChannelInterceptor`가 STOMP CONNECT 프레임에서 JWT를 검증한다.
- 퍼블릭 엔드포인트: 스웨거, OAuth 로그인, 토큰 재발급, `/api/auth/test/login`

### Real-time GPS Tracking Flow

1. 클라이언트가 WebSocket(STOMP)으로 GPS 좌표를 전송
2. `TrackingGpsPublisher`가 Redis Stream(`tracking:gps`)에 publish
3. `TrackingStreamConsumer`가 스트림을 consume → 실시간 통계(Redis Hash) 갱신 + 메모리 버퍼 적재
4. 버퍼가 100개 이상이거나 10초 주기 스케줄러가 동작하면 `TrackingPointFlushService`가 DB에 배치 insert
5. 세션 종료 시 `TrackingSessionTerminatedEvent`로 잔여 버퍼를 final flush

`TrackingPointFlushService`는 self-invocation AOP 우회를 위해 별도 빈으로 분리되어 있다.

### Database & Migrations

- PostgreSQL + PostGIS (지리 좌표 검색에 `hibernate-spatial` + JTS 사용)
- **prod 프로파일**: Flyway 활성화, `ddl-auto: validate`
- **local 프로파일**: prod와 동일하게 Flyway 활성화, `ddl-auto: validate` (마이그레이션 스크립트로 스키마 관리)
- 마이그레이션 스크립트: `src/main/resources/db/migration/V{N}__description.sql`

### Infrastructure

| 컴포넌트 | 용도 |
|---------|------|
| PostgreSQL | 메인 DB |
| Redis | JWT refresh token 블랙리스트, GPS 실시간 통계 (Hash), GPS 이벤트 스트림 (Stream) |
| MinIO | 이미지 오브젝트 스토리지 (presigned URL 방식) |
| Firebase FCM | 푸시 알림 |

### Environment Variables

`application.yaml`은 환경변수만 참조한다. 로컬 개발은 `application-local.yaml`에 기본값이 하드코딩되어 있다(로컬 전용).  
prod에서 필요한 주요 변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `KAKAO_ADMIN_KEY`, `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_PUBLIC_URL`, `TRACKING_STREAM_KEY`, `TRACKING_CONSUMER_GROUP`, `FIREBASE_SERVICE_ACCOUNT_PATH`, `DISCORD_ALERT_ENABLED`, `DISCORD_WEBHOOK_URL`

## Branch & Commit Convention

브랜치명:
```text
feat/#이슈번호-기능명
fix/#이슈번호-버그명
```

커밋 타입: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

## PR Rules

- 리뷰어 2명 전원 승인 후 머지 (셀프 머지 금지)

## Swagger

로컬 실행 후 `http://localhost:8080/swagger-ui.html` 에서 API 문서 확인 가능.
